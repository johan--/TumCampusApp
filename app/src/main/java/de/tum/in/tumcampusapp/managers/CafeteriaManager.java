package de.tum.in.tumcampusapp.managers;

import android.content.Context;
import android.text.format.DateUtils;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.tum.in.tumcampusapp.api.TUMCabeClient;
import de.tum.in.tumcampusapp.auxiliary.Utils;
import de.tum.in.tumcampusapp.cards.CafeteriaMenuCard;
import de.tum.in.tumcampusapp.cards.generic.Card;
import de.tum.in.tumcampusapp.database.TcaDb;
import de.tum.in.tumcampusapp.models.cafeteria.CafeteriaMenu;
import de.tum.in.tumcampusapp.repository.CafeteriaLocalRepository;
import de.tum.in.tumcampusapp.repository.CafeteriaRemoteRepository;
import de.tum.in.tumcampusapp.viewmodel.CafeteriaViewModel;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Cafeteria Manager, handles database stuff, external imports
 */
public class CafeteriaManager implements Card.ProvidesCard {

    private final Context mContext;
    private final CafeteriaViewModel cafeteriaViewModel;
    private final CompositeDisposable compositeDisposable;

    /**
     * Constructor, open/create database, create table if necessary
     *
     * @param context Context
     */
    public CafeteriaManager(Context context) {
        mContext = context;
        TcaDb db = TcaDb.getInstance(context);
        compositeDisposable = new CompositeDisposable();
        CafeteriaLocalRepository localRepository = CafeteriaLocalRepository.INSTANCE;
        localRepository.setDb(db);
        CafeteriaRemoteRepository remoteRepository = CafeteriaRemoteRepository.INSTANCE;
        remoteRepository.setTumCabeClient(TUMCabeClient.getInstance(context));
        cafeteriaViewModel = new CafeteriaViewModel(localRepository,remoteRepository,compositeDisposable);
    }

    /**
     * Shows card for the best matching cafeteria.
     *
     * @param context Context
     * @see LocationManager#getCafeteria()
     */
    @Override
    public void onRequestCard(Context context) {
        // Choose which mensa should be shown
        int cafeteriaId = new LocationManager(context).getCafeteria();
        if (cafeteriaId == -1) {
            return;
        }
        CafeteriaMenuCard card = new CafeteriaMenuCard(context);

        compositeDisposable.add(createCafeteriaObservable(cafeteriaId)
                .take(1).subscribe(cafeteria -> {
                    card.setCardMenus(cafeteria.id, cafeteria.name, cafeteria.dateStr, Utils.getDate(cafeteria.dateStr), cafeteria.menus);
                    card.apply();
                }, throwable -> Utils.log(throwable.getMessage())));

    }

    /**
     * returns the menus of the best matching cafeteria
     */
    public Flowable<Map<String, List<CafeteriaMenu>>> getBestMatchMensaInfo(Context context) {
        // Choose which mensa should be shown
        int cafeteriaId = new LocationManager(context).getCafeteria();
        if (cafeteriaId == -1) {
            Utils.log("could not get a Cafeteria form locationManager!");
            return Flowable.just(Collections.emptyMap());
        }

        return createCafeteriaObservable(cafeteriaId)
                .map(cafeteria -> {
                    String mensaKey = cafeteria.name + ' ' + cafeteria.dateStr;
                    Map<String, List<CafeteriaMenu>> selectedMensaMenus = new HashMap<>(1);
                    selectedMensaMenus.put(mensaKey, cafeteria.menus);
                    return selectedMensaMenus;
                });

    }

    public Flowable<String> getBestMatchMensaName(Context context) {
        // Choose which mensa should be shown
        int cafeteriaId = new LocationManager(context).getCafeteria();
        if (cafeteriaId == -1) {
            Utils.log("could not get a Cafeteria form locationManager!");
            return Flowable.just("");
        }
        return createCafeteriaObservable(cafeteriaId)
                .map(s -> s.name + ' ' + s.dateStr);

    }

    public int getBestMatchMensaId(Context context) {
        // Choose which mensa should be shown
        int cafeteriaId = new LocationManager(context).getCafeteria();
        if (cafeteriaId == -1) {
            Utils.log("could not get a Cafeteria form locationManager!");
        }
        return cafeteriaId;
    }

    private String createDateString(List<String> cafeteriaDates){
        Calendar now = Calendar.getInstance();
        String dateStr= cafeteriaDates.isEmpty() ? Utils.getDateTimeString(new Date()) : cafeteriaDates.get(0);
        Date date = Utils.getDate(dateStr);
        if (DateUtils.isToday(date.getTime()) && now.get(Calendar.HOUR_OF_DAY) >= 15 && cafeteriaDates.size() > 1) {
            dateStr = cafeteriaDates.get(1);
        }
        return dateStr;
    }

    private Flowable<Cafeteria> createCafeteriaObservable(int cafeteriaId){
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.id = cafeteriaId;

        return cafeteriaViewModel
                .getCafeteriaNameFromId(cafeteriaId)
                .doOnError(throwable -> Utils.log(throwable.getMessage()))
                .flatMap(cafeteriaName -> {
                    cafeteria.name = cafeteriaName;
                    return cafeteriaViewModel.getAllMenuDates();
                })
                .flatMap(menuDates -> {
                    cafeteria.menuDates = menuDates;
                    cafeteria.dateStr = createDateString(menuDates);
                    return cafeteriaViewModel.getCafeteriaMenu(cafeteria.id, cafeteria.dateStr);
                })
                .map(menus -> {
                    cafeteria.menus = menus;
                    return cafeteria;
                });
    }

    private class Cafeteria {
        List<String> menuDates;
        List<CafeteriaMenu> menus;
        String dateStr;
        String name;
        int id;
    }

}