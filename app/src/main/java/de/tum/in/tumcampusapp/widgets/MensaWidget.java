package de.tum.in.tumcampusapp.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import java.util.Date;

import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.activities.CafeteriaActivity;
import de.tum.in.tumcampusapp.auxiliary.Const;
import de.tum.in.tumcampusapp.auxiliary.Utils;
import de.tum.in.tumcampusapp.database.TcaDb;
import de.tum.in.tumcampusapp.managers.CafeteriaManager;
import de.tum.in.tumcampusapp.models.cafeteria.Cafeteria;
import de.tum.in.tumcampusapp.repository.CafeteriaLocalRepository;
import de.tum.in.tumcampusapp.services.MensaWidgetService;
import io.reactivex.Flowable;

/**
 * Implementation of Mensa Widget functionality.
 * The Update intervals is set to 10 hours in mensa_widget_info.xml
 */
public class MensaWidget extends AppWidgetProvider {

    AppWidgetManager appWidgetManager;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // There may be multiple widgets active, so update all of them
        this.appWidgetManager = appWidgetManager;

        for (int appWidgetId : appWidgetIds) {

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.mensa_widget);

            // set the header for the Widget layout
            CafeteriaManager mensaManager = new CafeteriaManager(context);
            CafeteriaLocalRepository localRepository = CafeteriaLocalRepository.INSTANCE;
            localRepository.setDb(TcaDb.getInstance(context));
            Flowable<Cafeteria> cafeteria = localRepository.getCafeteria(mensaManager.getBestMatchMensaId(context));
            String name = cafeteria.map(cafeteria1 -> cafeteria1.getName() + " " + Utils.getDateTimeString(new Date()))
                                   .blockingFirst();
            rv.setTextViewText(R.id.mensa_widget_header, name);


            // set the header on click to open the mensa activity
            Intent mensaIntent = new Intent(context, CafeteriaActivity.class);
            mensaIntent.putExtra(Const.CAFETERIA_ID, mensaManager.getBestMatchMensaId(context));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, mensaIntent, 0);
            rv.setOnClickPendingIntent(R.id.mensa_widget_header_container, pendingIntent);

            // set the adapter for the list view in the mensaWidget
            Intent intent = new Intent(context, MensaWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            rv.setRemoteAdapter(R.id.food_item, intent); //appWidgetIds[i],
            rv.setEmptyView(R.id.empty_view, R.id.empty_view);
            appWidgetManager.updateAppWidget(appWidgetId, rv);

        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

}


