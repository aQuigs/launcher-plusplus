package com.sqftware.orbitlauncher.apps

import android.os.Process
import android.os.UserHandle
import android.os.UserManager

/** [user] as an app's work profile is held: null for the user's own profile, else that profile's serial number. */
internal fun UserManager.workProfileOf(user: UserHandle): Long? =
    if (user == Process.myUserHandle()) null else getSerialNumberForUser(user)
