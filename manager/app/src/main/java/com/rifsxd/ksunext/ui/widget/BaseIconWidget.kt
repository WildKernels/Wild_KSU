package com.rifsxd.ksunext.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.MainActivity

abstract class BaseIconWidget(
    private val iconResId: Int,
    private val action: String,
    private val labelResId: Int
) : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_icon)
        
        views.setImageViewResource(R.id.widget_button, iconResId)
        views.setContentDescription(R.id.widget_button, context.getString(labelResId))

        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = this@BaseIconWidget.action
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
