package de.tum.`in`.tumcampusapp.database

import android.arch.persistence.room.TypeConverter
import com.google.gson.Gson
import de.tum.`in`.tumcampusapp.auxiliary.Utils
import de.tum.`in`.tumcampusapp.models.gcm.GCMNotificationLocation
import de.tum.`in`.tumcampusapp.models.tumcabe.ChatMember
import java.util.*

class Converters {
    @TypeConverter
    fun fromIso(str: String): Date = Utils.getDateTime(str)

    @TypeConverter
    fun fromDate(date: Date): String = Utils.getDateTimeString(date)

    @TypeConverter
    fun toJson(location: GCMNotificationLocation): String = Gson().toJson(location);

    @TypeConverter
    fun toLocation(json: String): GCMNotificationLocation = Gson().fromJson(json, GCMNotificationLocation::class.java)

    @TypeConverter
    fun fromMember(member: ChatMember): String {
        return Gson().toJson(member)
    }

    @TypeConverter
    fun toMember(member: String): ChatMember {
        return Gson().fromJson<ChatMember>(member, ChatMember::class.java!!)
    }
}
