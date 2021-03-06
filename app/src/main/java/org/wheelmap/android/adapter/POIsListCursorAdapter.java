/*
 * #%L
 * Wheelmap - App
 * %%
 * Copyright (C) 2011 - 2012 Michal Harakal - Michael Kroez - Sozialhelden e.V.
 * %%
 * Wheelmap App based on the Wheelmap Service by Sozialhelden e.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wheelmap.android.adapter;

import org.wheelmap.android.app.WheelmapApp;
import org.wheelmap.android.manager.SupportManager;
import org.wheelmap.android.manager.SupportManager.NodeType;
import org.wheelmap.android.model.DirectionCursorWrapper;
import org.wheelmap.android.model.POIHelper;
import org.wheelmap.android.model.POIsCursorWrapper;
import org.wheelmap.android.model.WheelchairFilterState;
import org.wheelmap.android.view.POIsListItemView;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

public class POIsListCursorAdapter extends CursorAdapter {

    private Context mContext;

    private final static String TAG = POIsListCursorAdapter.class
            .getSimpleName();

    private DistanceFormatter mDistanceFormatter;

    public POIsListCursorAdapter(Context context, Cursor cursor,
            boolean autorequery, boolean useAngloDistanceUnit) {
        super(context, cursor, autorequery);
        changeAdapter(useAngloDistanceUnit);
        mContext = context;
    }

    public void changeAdapter(boolean useAngloDistanceUnit) {
        if (useAngloDistanceUnit) {
            mDistanceFormatter = new DistanceFormatterAnglo();
        } else {
            mDistanceFormatter = new DistanceFormatterMetric();
        }

        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        POIsListItemView pliv = (POIsListItemView) view;
        SupportManager manager = WheelmapApp.getSupportManager();

        String name = POIHelper.getName(cursor);
        WheelchairFilterState state = POIHelper.getWheelchair(cursor);
        int index = cursor
                .getColumnIndex(POIsCursorWrapper.LOCATION_COLUMN_NAME);
        float distance = cursor.getFloat(index);
        float direction = cursor
                .getFloat(cursor
                        .getColumnIndex(DirectionCursorWrapper.SHOW_DIRECTION_COLUMN_NAME));
        int categoryId = POIHelper.getCategoryId(cursor);
        int nodeTypeId = POIHelper.getNodeTypeId(cursor);
        NodeType nodeType = manager.lookupNodeType(nodeTypeId);

        if (name != null && name.length() > 0) {
            pliv.setName(name);
        } else {
            pliv.setName(nodeType.localizedName);
        }

        pliv.setNodeType(nodeType.localizedName);

        pliv.setDistance(mDistanceFormatter.format(distance));
        Drawable marker = manager.lookupNodeTypeList(nodeTypeId).getStateDrawable(state);

        pliv.setIcon(marker);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new POIsListItemView(context, POIHelper.getName(cursor));
    }

    private interface DistanceFormatter {

        String format(float distance);
    }

    private static class DistanceFormatterMetric implements DistanceFormatter {

        @Override
        public String format(float distance) {
            if (distance < 1.0) {
                return String.format("%2.0f0m", distance * 100.0);
            } else {
                return String.format("%.1fkm", distance);
            }
        }
    }

    private static class DistanceFormatterAnglo implements DistanceFormatter {

        @Override
        public String format(float distance) {

            if (distance < 1.0) {
                return String.format("%2.0f0yd", distance * 176.0);
            } else {
                return String.format("%.1fmi", distance);
            }
        }
    }

}
