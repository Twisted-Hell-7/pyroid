package com.pythonide.data.runtime.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Idle)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val _grantedPermissions = MutableStateFlow<Set<String>>(emptySet())
    val grantedPermissions: StateFlow<Set<String>> = _grantedPermissions.asStateFlow()

    private val _deniedPermissions = MutableStateFlow<Set<String>>(emptySet())
    val deniedPermissions: StateFlow<Set<String>> = _deniedPermissions.asStateFlow()

    sealed class PermissionState {
        object Idle : PermissionState()
        data class Checking(val permissions: List<String>) : PermissionState()
        data class Granted(val permissions: List<String>) : PermissionState()
        data class Denied(val permissions: List<String>, val shouldShowRationale: Boolean) : PermissionState()
        data class PermanentlyDenied(val permissions: List<String>) : PermissionState()
    }

    data class AppPermission(
        val name: String,
        val description: String,
        val isRequired: Boolean,
        val category: PermissionCategory
    )

    enum class PermissionCategory {
        FILE_ACCESS, NETWORK, CAMERA, MICROPHONE, LOCATION, STORAGE, OTHER
    }

    private val requiredPermissions = listOf(
        AppPermission(
            name = Manifest.permission.READ_EXTERNAL_STORAGE,
            description = "Read files from storage",
            isRequired = true,
            category = PermissionCategory.FILE_ACCESS
        ),
        AppPermission(
            name = Manifest.permission.WRITE_EXTERNAL_STORAGE,
            description = "Write files to storage",
            isRequired = true,
            category = PermissionCategory.FILE_ACCESS
        ),
        AppPermission(
            name = Manifest.permission.INTERNET,
            description = "Access the internet for package installation",
            isRequired = false,
            category = PermissionCategory.NETWORK
        ),
        AppPermission(
            name = Manifest.permission.ACCESS_NETWORK_STATE,
            description = "Check network connectivity status",
            isRequired = false,
            category = PermissionCategory.NETWORK
        )
    )

    private val optionalPermissions = listOf(
        AppPermission(
            name = Manifest.permission.CAMERA,
            description = "Scan QR codes for project import",
            isRequired = false,
            category = PermissionCategory.CAMERA
        ),
        AppPermission(
            name = Manifest.permission.RECORD_AUDIO,
            description = "Voice input for code dictation",
            isRequired = false,
            category = PermissionCategory.MICROPHONE
        )
    )

    fun checkPermission(permission: String): PermissionStatus {
        return when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                PermissionStatus.GRANTED
            }
            shouldShowRationale(permission) -> {
                PermissionStatus.DENIED_SHOULD_SHOW_RATIONALE
            }
            else -> {
                PermissionStatus.DENIED
            }
        }
    }

    fun checkAllPermissions(): Map<String, PermissionStatus> {
        val allPermissions = requiredPermissions + optionalPermissions
        return allPermissions.associate { it.name to checkPermission(it.name) }
    }

    fun getRequiredPermissions(): List<AppPermission> = requiredPermissions

    fun getOptionalPermissions(): List<AppPermission> = optionalPermissions

    fun getPermissionsByCategory(category: PermissionCategory): List<AppPermission> {
        return (requiredPermissions + optionalPermissions).filter { it.category == category }
    }

    fun shouldRequestPermission(permission: String): Boolean {
        val status = checkPermission(permission)
        return status != PermissionStatus.GRANTED
    }

    fun getMissingPermissions(): List<AppPermission> {
        return (requiredPermissions + optionalPermissions).filter {
            checkPermission(it.name) != PermissionStatus.GRANTED
        }
    }

    fun getMissingRequiredPermissions(): List<AppPermission> {
        return requiredPermissions.filter {
            checkPermission(it.name) != PermissionStatus.GRANTED
        }
    }

    fun updatePermissionStatus(permission: String, granted: Boolean) {
        val currentGranted = _grantedPermissions.value.toMutableSet()
        val currentDenied = _deniedPermissions.value.toMutableSet()

        if (granted) {
            currentGranted.add(permission)
            currentDenied.remove(permission)
        } else {
            currentGranted.remove(permission)
            currentDenied.add(permission)
        }

        _grantedPermissions.value = currentGranted
        _deniedPermissions.value = currentDenied
    }

    fun canAccessFiles(): Boolean {
        return checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PermissionStatus.GRANTED &&
                checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PermissionStatus.GRANTED
    }

    fun canAccessNetwork(): Boolean {
        return checkPermission(Manifest.permission.INTERNET) == PermissionStatus.GRANTED
    }

    fun canUseCamera(): Boolean {
        return checkPermission(Manifest.permission.CAMERA) == PermissionStatus.GRANTED
    }

    fun getPermissionRationale(permission: String): String? {
        return when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                "Storage permission is required to read Python files and projects"
            }
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                "Storage permission is required to save Python files and projects"
            }
            Manifest.permission.INTERNET -> {
                "Internet permission is required to install Python packages and dependencies"
            }
            Manifest.permission.CAMERA -> {
                "Camera permission is needed to scan QR codes for project import"
            }
            Manifest.permission.RECORD_AUDIO -> {
                "Audio permission is needed for voice input code dictation"
            }
            else -> null
        }
    }

    private fun shouldShowRationale(permission: String): Boolean {
        return try {
            val activity = (context as? android.app.Activity)
            activity?.shouldShowRequestPermissionRationale(permission) ?: false
        } catch (e: Exception) {
            false
        }
    }

    enum class PermissionStatus {
        GRANTED,
        DENIED,
        DENIED_SHOULD_SHOW_RATIONALE,
        DENIED_PERMANENTLY
    }

    fun isPermissionPermanentlyDenied(permission: String): Boolean {
        return checkPermission(permission) == PermissionStatus.DENIED &&
                !shouldShowRationale(permission)
    }

    fun getPermissionsSummary(): PermissionSummary {
        val allPermissions = requiredPermissions + optionalPermissions
        val granted = allPermissions.filter { checkPermission(it.name) == PermissionStatus.GRANTED }
        val denied = allPermissions.filter { checkPermission(it.name) != PermissionStatus.GRANTED }
        val permanentlyDenied = allPermissions.filter { isPermissionPermanentlyDenied(it.name) }

        return PermissionSummary(
            total = allPermissions.size,
            granted = granted.size,
            denied = denied.size,
            permanentlyDenied = permanentlyDenied.size,
            requiredGranted = requiredPermissions.all { checkPermission(it.name) == PermissionStatus.GRANTED },
            details = allPermissions.map { permission ->
                PermissionDetail(
                    permission = permission,
                    status = checkPermission(permission.name),
                    rationale = getPermissionRationale(permission.name)
                )
            }
        )
    }

    data class PermissionSummary(
        val total: Int,
        val granted: Int,
        val denied: Int,
        val permanentlyDenied: Int,
        val requiredGranted: Boolean,
        val details: List<PermissionDetail>
    )

    data class PermissionDetail(
        val permission: AppPermission,
        val status: PermissionStatus,
        val rationale: String?
    )
}
