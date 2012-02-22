/*
 * Copyright (C) 2010-2011 Geometer Plus <contact@geometerplus.com>
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

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;

import org.geometerplus.android.fbreader.api.PluginApi;

public class TestActivity extends Activity {
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		final Intent intent = getIntent();
		if ("android.fbreader.action.ADD_OPDS_CATALOG".equals(intent.getAction())) {
			ArrayList<PluginApi.MenuActionInfo> actions =
				intent.<PluginApi.MenuActionInfo>getParcelableArrayListExtra(
					PluginApi.PluginInfo.KEY
				);
			if (actions == null) {
				actions = new ArrayList<PluginApi.MenuActionInfo>();
			}
			final String baseUrl = intent.getData().toString();
			actions.add(new PluginApi.MenuActionInfo(
				Uri.parse(baseUrl + "/scanLocalNetwork"),
				getText(R.string.scan_local_network_menu_item).toString(),
				3
			));
			intent.putExtra(PluginApi.PluginInfo.KEY, actions);
			if (!startNextMatchingActivity(intent)) {
				setResult(1, intent);
			}
		}
		finish();
	}
}
