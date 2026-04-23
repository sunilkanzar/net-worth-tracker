package com.kanzar.networthtracker.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import androidx.core.content.ContextCompat;
import com.kanzar.networthtracker.MainActivity;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Tools;
import io.realm.Realm;

public final class OverviewWidget extends AppWidgetProvider {
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_overview);
        
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        remoteViews.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent);
        
        Month month = new Month().getLast();
        if (month == null) {
            month = new Month();
        }
        remoteViews.setTextViewText(R.id.widgetHeader, month.toString());
        
        double percent;
        double change;
        boolean hasAssets;
        try (Realm realm = Realm.getDefaultInstance()) {
            percent = month.getPercent(realm);
            change = month.getValue() - month.getPreviousMonth(realm).getValue();
            hasAssets = month.hasAssets(realm);
        }

        remoteViews.setTextViewText(R.id.widgetPercent, Tools.formatPercent(percent));
        
        if (hasAssets) {
            remoteViews.setViewVisibility(R.id.widgetEmpty, android.view.View.GONE);
            remoteViews.setViewVisibility(R.id.changeView, android.view.View.VISIBLE);
        } else {
            remoteViews.setViewVisibility(R.id.widgetEmpty, android.view.View.VISIBLE);
            remoteViews.setViewVisibility(R.id.changeView, android.view.View.GONE);
        }
        
        remoteViews.setTextColor(R.id.widgetPercent, ContextCompat.getColor(context, Tools.getTextChangeColor(change)));
        
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }
}
