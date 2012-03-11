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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class LocalIPActivity extends Activity {
	private final int[] myMinAddress = new int[4];
	private final int[] myMaxAddress = new int[4];
	private final int[] myAddressViewIds = {
		R.id.local_ip_address_0,
		R.id.local_ip_address_1,
		R.id.local_ip_address_2,
		R.id.local_ip_address_3
	};

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.local_ip);

		setTitle(R.string.local_ip_window_title);

		final View buttonView = findViewById(R.id.local_ip_buttons);

		final Button cancelButton = (Button)buttonView.findViewById(R.id.cancel_button);
		cancelButton.setText(R.string.button_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				finish();
			}
		});

		final Button okButton = (Button)buttonView.findViewById(R.id.ok_button);
		okButton.setText(R.string.button_ok);
		okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				try {
					final StringBuilder buffer = new StringBuilder("http://");
					for (int i = 0; i < 4; ++i) {
						buffer.append(getViewText(myAddressViewIds[i])).append(i < 3 ? "." : ":");
					}
					buffer.append(getViewText(R.id.local_ip_port)).append("/opds");
					startActivity(
						new Intent("android.fbreader.action.ADD_OPDS_CATALOG_URL")
							.setData(Uri.parse(buffer.toString()))
							.putExtra("type", 2)
					);
					finish();
				} catch (ActivityNotFoundException e) {
					e.printStackTrace();
				}
			}
		});
		okButton.setEnabled(false);

		setViewText(R.id.local_ip_address_label, R.string.address_label);
		setViewText(R.id.local_ip_port_label, R.string.port_label);

		Arrays.fill(myMinAddress, 0);
		myMinAddress[0] = 1;
		Arrays.fill(myMaxAddress, 255);

		final List<InterfaceAddress> addresses = Util.getInterfaceAddresses();
		if (addresses.size() == 1) {
			final InterfaceAddress address = addresses.get(1);
			final byte[] addressBytes = address.getAddress().getAddress();
			int len = address.getNetworkPrefixLength();
			for (int index = 0; index < 4 && len > 0; ++index, len -= 8) {
				final int addressPart = addressBytes[index] & 0xFF;
				final int mask = (1 << Math.max(0, 8 - len)) - 1;
				setViewText(myAddressViewIds[index], String.valueOf(addressPart));
				myMinAddress[index] = addressPart & ~mask;
				myMaxAddress[index] = addressPart | mask;
				if (len >= 8) {
					disable(myAddressViewIds[index]);
				}
			}
			setViewText(R.id.local_ip_port, String.valueOf(8080));
		}
	}

	private void setViewText(int resourceId, String text) {
		((TextView)findViewById(resourceId)).setText(text);
	}

	private void setViewText(int resourceId, int textId) {
		((TextView)findViewById(resourceId)).setText(textId);
	}

	private String getViewText(int resourceId) {
		return ((TextView)findViewById(resourceId)).getText().toString();
	}

	private void disable(int resourceId) {
		final View view = findViewById(resourceId);
		view.setEnabled(false);
		view.setFocusable(false);
	}

	/*
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
	*/
	private Timer myOkButtonUpdater;

	@Override
	protected void onResume() {
		super.onResume();
		if (myOkButtonUpdater == null) {
			myOkButtonUpdater = new Timer();
			myOkButtonUpdater.schedule(new TimerTask() {
				public void run() {
					runOnUiThread(new Runnable() {
						public void run() {
							checkInputView();
						}
					});
				}
			}, 0, 100);
		}
	}

	@Override
	protected void onPause() {
		if (myOkButtonUpdater != null) {
			myOkButtonUpdater.cancel();
			myOkButtonUpdater.purge();
			myOkButtonUpdater = null;
		}
		super.onPause();
	}

	private boolean inRange(int viewId, int min, int max) {
		try {
			final int num = Integer.parseInt(getViewText(viewId));
			return min <= num && num <= max;
		} catch (Exception e) {
			return false;
		}
	}

	private void checkInputView() {
		findViewById(R.id.ok_button).setEnabled(
			inRange(myAddressViewIds[0], myMinAddress[0], myMaxAddress[0]) &&
			inRange(myAddressViewIds[1], myMinAddress[1], myMaxAddress[1]) &&
			inRange(myAddressViewIds[2], myMinAddress[2], myMaxAddress[2]) &&
			inRange(myAddressViewIds[3], myMinAddress[3], myMaxAddress[3]) &&
			inRange(R.id.local_ip_port, 1, 65535)
		);
	}
}
