/*
 * Copyright (C) 2010-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.plugin.local_opds_scanner;

import java.util.*;
import java.net.*;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpHead;

import android.app.ListActivity;
import android.content.*;
import android.graphics.Color;
import android.net.*;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import javax.jmdns.*;

public class ScanLocalNetworkActivity extends ListActivity {
	private WifiManager.MulticastLock myLock;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.scan_local_network);

		setListAdapter(new ItemAdapter());

		setTitle(R.string.window_title);

		final View buttonView = findViewById(R.id.scan_local_network_buttons);

		final Button cancelButton = (Button)buttonView.findViewById(R.id.cancel_button);
		cancelButton.setText(R.string.button_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				finish();
			}
		});

		final WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		final int state = wifiManager.getWifiState();
		if (state != WifiManager.WIFI_STATE_ENABLED && state != WifiManager.WIFI_STATE_ENABLING) {
			setTitle(R.string.wifi_is_turned_off);
			final View listView = findViewById(android.R.id.list);
			final TextView errorView = (TextView)findViewById(R.id.scan_local_network_error);
			listView.setVisibility(View.GONE);
			errorView.setVisibility(View.VISIBLE);
			errorView.setText(R.string.turn_wifi_on);

			final Button turnOnButton = (Button)buttonView.findViewById(R.id.ok_button);
			turnOnButton.setText(R.string.button_turn_on);
			turnOnButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					wifiManager.setWifiEnabled(true);
					finish();
				}
			});

			myLock = null;
		} else {
			final Button rescanButton = (Button)buttonView.findViewById(R.id.ok_button);
			rescanButton.setText(R.string.button_rescan);
			rescanButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					runOnUiThread(new Runnable() {
						public void run() {
							clear();
							scan();
						}
					});
				}
			});

			myLock = wifiManager.createMulticastLock("FBReader_lock");
			myLock.setReferenceCounted(true);
			myLock.acquire();

			scan();
		}
	}

	@Override
	protected void onDestroy() {
		if (myLock != null) {
			myLock.release();
		}
		super.onDestroy();
	}

	private List<InetAddress> getLocalIpAddresses() {
		final List<InetAddress> addresses = new LinkedList<InetAddress>();
		try {
			for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
					if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
						addresses.add(addr);
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return addresses;
	}

	private class ServiceCollector implements ServiceListener {
		private final static String STANZA_ZEROCONF_TYPE = "_stanza._tcp.local.";
		private final static String CALIBRE_ZEROCONF_TYPE = "_calibre._tcp.local.";
		private final static String OPDS_ZEROCONF_TYPE = "_opds._tcp.local.";

		private JmDNS myMCDNS;

		ServiceCollector(InetAddress address) {
			try {
				myMCDNS = JmDNS.create(address, "FBReader");
			} catch (IOException e) {
				return;
			}
			myMCDNS.addServiceListener(STANZA_ZEROCONF_TYPE, this);
			myMCDNS.addServiceListener(CALIBRE_ZEROCONF_TYPE, this);
			myMCDNS.addServiceListener(OPDS_ZEROCONF_TYPE, this);

			runOnUiThread(new Runnable() {
				public void run() {
					getListAdapter().addWaitItem();
				}
			});
			final Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					runOnUiThread(new Runnable() {
						public void run() {
							final ItemAdapter adapter = getListAdapter();
							if (adapter.removeWaitItem() && adapter.getCount() == 0) {
								setErrorText(R.string.no_catalogs_found);
							}
						}
					});
					try {
						myMCDNS.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					timer.cancel();
				}
			}, 10000);
		}

		public void serviceAdded(ServiceEvent event) {
			ServiceInfo info = event.getInfo();
			if (info == null || !info.hasData()) {
				info = myMCDNS.getServiceInfo(event.getType(), event.getName(), true);
			}
			addInfo(info);
		}

		public void serviceRemoved(ServiceEvent event) {
			// TODO
		}

		public void serviceResolved(ServiceEvent event) {
			addInfo(event.getInfo());
		}

		private void addInfo(final ServiceInfo info) {
			if (info == null || !info.hasData()) {
				return;
			}

			final String path = info.getPropertyString("path");
			if (path == null) {
				return;
			}

			for (String url : info.getURLs()) {
				if (url == null || !url.endsWith(path)) {
					continue;
				}

				final String type = info.getType();
				if (STANZA_ZEROCONF_TYPE.equals(info.getType()) || "/stanza".equals(path)) {
					url = url.substring(0, url.length() - path.length()) + "/opds";
				}

				boolean verified = false;
				final DefaultHttpClient httpClient = new DefaultHttpClient();
				final HttpHead httpRequest = new HttpHead(url);
				httpRequest.setHeader("Accept-Language", Locale.getDefault().getLanguage());
				for (int retryCounter = 0; retryCounter < 3; ++retryCounter) {
					try {
						final HttpResponse response = httpClient.execute(httpRequest);
						if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
							verified = true;
							break;
						}
					} catch (IOException e) {
					}
				}

				if (verified) {
					final String serviceUrl = url;
					runOnUiThread(new Runnable() {
						public void run() {
							getListAdapter().addServiceItem(
								info.getName(),
								serviceUrl,
								R.drawable.ic_list_library_calibre
							);
						}
					});
				}
			}
		}
	}

	private void scan() {
		final List<InetAddress> addresses = getLocalIpAddresses();
		if (addresses.isEmpty()) {
			runOnUiThread(new Runnable() {
				public void run() {
					setErrorText(R.string.no_local_connection);
				}
			});
		} else {
			for (final InetAddress a : addresses) {
				new Thread() {
					public void run() {
						new ServiceCollector(a);
					}
				}.start();
			}
		}
	}

	private void clear() {
		getListAdapter().clear();
		final View listView = findViewById(android.R.id.list);
		final TextView errorView = (TextView)findViewById(R.id.scan_local_network_error);
		listView.setVisibility(View.VISIBLE);
		errorView.setVisibility(View.GONE);
	}

	private void setErrorText(final int errorTextId) {
		final View listView = findViewById(android.R.id.list);
		final TextView errorView = (TextView)findViewById(R.id.scan_local_network_error);
		listView.setVisibility(View.GONE);
		errorView.setVisibility(View.VISIBLE);
		errorView.setText(errorTextId);
	}

	private class ItemAdapter extends BaseAdapter {
		final private WaitItem myWaitItem = new WaitItem(
			getText(R.string.scanning_local_network).toString()
		);
		private volatile int myWaitItemCount;
		private final ArrayList<ServiceInfoItem> myItems = new ArrayList<ServiceInfoItem>();

		@Override
		public Item getItem(int position) {
			try {
				return myItems.get(position);
			} catch (Exception e) {
				return myWaitItem;
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public synchronized int getCount() {
			return myWaitItemCount > 0 ? myItems.size() + 1 : myItems.size();
		}

		synchronized void clear() {
			myItems.clear();
			myWaitItemCount = 0;
			notifyDataSetChanged();
		}

		synchronized boolean addWaitItem() {
			if (myWaitItemCount++ == 0) {
				notifyDataSetChanged();
				findViewById(R.id.scan_local_network_container).invalidate();
				return true;
			}
			return false;
		}

		synchronized boolean removeWaitItem() {
			if (myWaitItemCount == 0) {
				return false;
			}
			if (--myWaitItemCount == 0) {
				notifyDataSetChanged();
				findViewById(R.id.scan_local_network_container).invalidate();
				return true;
			}
			return false;
		}

		synchronized void addServiceItem(String name, String url, int iconId) {
			try {
				final ServiceInfoItem item = new ServiceInfoItem(name, Uri.parse(url), iconId);
				if (!myItems.contains(item)) {
					myItems.add(item);
					notifyDataSetChanged();
					findViewById(R.id.scan_local_network_container).invalidate();
				}
			} catch (ParseException e) {
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Item item = getItem(position);
			final View view;
			if (convertView == null) {
				view = LayoutInflater.from(ScanLocalNetworkActivity.this).inflate(R.layout.local_service_item, parent, false);
			} else {
				view = convertView;
			}

			final TextView textView = (TextView)view.findViewById(R.id.local_service_text);
			final ImageView iconView = (ImageView)view.findViewById(R.id.local_service_icon);
			final ProgressBar progress = (ProgressBar)view.findViewById(R.id.local_service_progress);
			if (convertView == null) {
				view.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				final int h = view.getMeasuredHeight() * 6 / 10;
				iconView.getLayoutParams().width = h;
				iconView.getLayoutParams().height = h;
				iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				iconView.requestLayout();
				progress.getLayoutParams().width = h;
				progress.getLayoutParams().height = h;
				iconView.requestLayout();
			}

			textView.setText(item.Name);

			if (item instanceof ServiceInfoItem) {
				iconView.setVisibility(View.VISIBLE);
				progress.setVisibility(View.GONE);
				iconView.setImageResource(((ServiceInfoItem)item).IconId);
			} else /* item instanceof WaitItem */ {
				iconView.setVisibility(View.GONE);
				progress.setVisibility(View.VISIBLE);
			}

			return view;
		}
	}

	private static abstract class Item {
		public final String Name;

		public Item(String name) {
			Name = name;
		}
	}

	private static class ServiceInfoItem extends Item {
		public final Uri URI;
		public final int IconId;

		public ServiceInfoItem(String name, Uri uri, int iconId) {
			super(name);
			URI = uri;
			IconId = iconId;
		}

		@Override
		public int hashCode() {
			return Name.hashCode() + URI.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return
				o instanceof ServiceInfoItem &&
				Name.equals(((ServiceInfoItem)o).Name) &&
				URI.equals(((ServiceInfoItem)o).URI);
		}
	}

	private static class WaitItem extends Item {
		public WaitItem(String name) {
			super(name);
		}
	}

	@Override
	protected void onListItemClick(ListView parent, View view, int position, long id) {
		final Item item = getListAdapter().getItem(position);
		if (item instanceof ServiceInfoItem) {
			try {
				startActivity(
					new Intent("android.fbreader.action.ADD_OPDS_CATALOG_URL")
						.setData(((ServiceInfoItem)item).URI)
						.putExtra("type", 2)
				);
				finish();
			} catch (ActivityNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public ItemAdapter getListAdapter() {
		return (ItemAdapter)super.getListAdapter();
	}
}
