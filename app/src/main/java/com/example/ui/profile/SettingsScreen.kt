package com.example.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.theme.ThemeManager

enum class SettingsSubScreen {
    MAIN,
    EDIT_PROFILE,
    APPEARANCE,
    PRIVACY,
    NOTIFICATIONS,
    ACCOUNT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onChangePassword: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    // Edit Profile form fields
    var displayName by remember(uiState.user) { mutableStateOf(uiState.user?.displayName ?: "") }
    var username by remember(uiState.user) { mutableStateOf(uiState.user?.username ?: "") }
    var bio by remember(uiState.user) { mutableStateOf(uiState.user?.bio ?: "") }
    var photoUrl by remember(uiState.user) { mutableStateOf(uiState.user?.photoUrl ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Privacy and notifications toggle states
    var isPrivateAccount by remember { mutableStateOf(false) }
    var showLastSeen by remember { mutableStateOf(true) }
    var notifyMessage by remember { mutableStateOf(true) }
    var notifyGroup by remember { mutableStateOf(true) }
    var fontScaleState by remember { mutableStateOf("متوسط") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Handles back navigation based on sub-screen
    val handleBackPress = {
        if (currentSubScreen == SettingsSubScreen.MAIN) {
            onNavigateBack()
        } else {
            currentSubScreen = SettingsSubScreen.MAIN
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentSubScreen) {
                            SettingsSubScreen.MAIN -> "الملف الشخصي والضبط"
                            SettingsSubScreen.EDIT_PROFILE -> "تعديل الملف الشخصي"
                            SettingsSubScreen.APPEARANCE -> "المظهر والسمة"
                            SettingsSubScreen.PRIVACY -> "الخصوصية والأمان"
                            SettingsSubScreen.NOTIFICATIONS -> "الإشعارات والأصوات"
                            SettingsSubScreen.ACCOUNT -> "إدارة الحساب"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentSubScreen,
                transitionSpec = {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                },
                label = "SettingsScreenTransition"
            ) { targetScreen ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (targetScreen) {
                        SettingsSubScreen.MAIN -> {
                            // LARGE HERO AVATAR
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = uiState.user?.photoUrl?.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${uiState.user?.displayName ?: "User"}" } ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=User",
                                    contentDescription = "صورة الحساب",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // User details
                            Text(
                                text = uiState.user?.displayName ?: "الاسم الكريم",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            
                            if (!uiState.user?.username.isNullOrBlank()) {
                                Text(
                                    text = "@${uiState.user?.username}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            if (!uiState.user?.email.isNullOrBlank()) {
                                Text(
                                    text = uiState.user?.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Bio box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = uiState.user?.bio?.ifEmpty { "لا توجد نبذة شخصية حالياً. اكتب نبذتك لتعريف الآخرين بك." } ?: "لا توجد نبذة شخصية حالياً. اكتب نبذتك لتعريف الآخرين بك.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(28.dp))
                            
                            // MENU ITEMS
                            SettingsMenuItem(
                                title = "تعديل الملف الشخصي",
                                subtitle = "الاسم، اسم المستخدم والنبذة التعريفية",
                                icon = Icons.Default.Edit,
                                onClick = { currentSubScreen = SettingsSubScreen.EDIT_PROFILE }
                            )
                            SettingsMenuItem(
                                title = "الخصوصية والأمان",
                                subtitle = "حالة النشاط، الحظر وإدارة التشفير",
                                icon = Icons.Default.Shield,
                                onClick = { currentSubScreen = SettingsSubScreen.PRIVACY }
                            )
                            SettingsMenuItem(
                                title = "الإشعارات والأصوات",
                                subtitle = "تنبيهات المحادثات الفردية والجماعية",
                                icon = Icons.Default.Notifications,
                                onClick = { currentSubScreen = SettingsSubScreen.NOTIFICATIONS }
                            )
                            SettingsMenuItem(
                                title = "المظهر والسمة",
                                subtitle = "الوضع الداكن، الألوان وحجم خط القراءة",
                                icon = Icons.Default.Palette,
                                onClick = { currentSubScreen = SettingsSubScreen.APPEARANCE }
                            )
                            SettingsMenuItem(
                                title = "حسابي وإعدادات المرور",
                                subtitle = "إدارة الأمان وكلمات السر",
                                icon = Icons.Default.AccountBox,
                                onClick = { currentSubScreen = SettingsSubScreen.ACCOUNT }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // LOGOUT BUTTON (Red accent)
                            Button(
                                onClick = { 
                                    viewModel.logout()
                                    onLogout()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تسجيل الخروج", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        
                        SettingsSubScreen.EDIT_PROFILE -> {
                            // Edit Profile Sub-Screen
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = selectedImageUri ?: photoUrl.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${displayName}" },
                                    contentDescription = "تغيير الصورة",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("اضغط لتحديث الصورة الشخصية", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("الاسم") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("اسم المستخدم") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = { Text("النبذة الشخصية (Bio)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { 
                                    viewModel.updateProfile(displayName, username.trim(), bio, photoUrl, selectedImageUri)
                                    currentSubScreen = SettingsSubScreen.MAIN
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                enabled = !uiState.isSaving && displayName.isNotBlank() && username.isNotBlank(),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        SettingsSubScreen.APPEARANCE -> {
                            // Appearance Settings Sub-Screen
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("اختيار المظهر", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                                    
                                    ThemeOptionRow(
                                        title = "مظهر فاتح",
                                        icon = Icons.Default.LightMode,
                                        selected = ThemeManager.themeMode.value == "LIGHT",
                                        onClick = { ThemeManager.setTheme(context, "LIGHT") }
                                    )
                                    ThemeOptionRow(
                                        title = "مظهر داكن",
                                        icon = Icons.Default.DarkMode,
                                        selected = ThemeManager.themeMode.value == "DARK",
                                        onClick = { ThemeManager.setTheme(context, "DARK") }
                                    )
                                    ThemeOptionRow(
                                        title = "تلقائي (حسب النظام)",
                                        icon = Icons.Default.SettingsSuggest,
                                        selected = ThemeManager.themeMode.value == "SYSTEM",
                                        onClick = { ThemeManager.setTheme(context, "SYSTEM") }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("لون التميز والمؤشر", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val colors = listOf(
                                            Color(0xFF2563EB), // blue
                                            Color(0xFF7C3AED), // violet
                                            Color(0xFF22C55E), // green
                                            Color(0xFF06B6D4), // cyan
                                            Color(0xFFF97316), // orange
                                            Color(0xFFEC4899)  // pink
                                        )
                                        colors.forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(color, CircleShape)
                                                    .border(2.dp, Color.White, CircleShape)
                                                    .clickable { /* Select accent color placeholder */ }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("حجم الخط للمحادثة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                                    
                                    val sizes = listOf("صغير", "متوسط", "كبير")
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        sizes.forEach { size ->
                                            val isSel = fontScaleState == size
                                            Button(
                                                onClick = { fontScaleState = size },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(size, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        SettingsSubScreen.PRIVACY -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("خيارات الخصوصية", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("حساب خاص", fontWeight = FontWeight.Medium)
                                            Text("إخفاء الحساب عن غير جهات الاتصال", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = isPrivateAccount, onCheckedChange = { isPrivateAccount = it })
                                    }
                                    
                                    HorizontalDivider()
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("آخر ظهور وحالة النشاط", fontWeight = FontWeight.Medium)
                                            Text("السماح للآخرين بمعرفة وقت تواجدك", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = showLastSeen, onCheckedChange = { showLastSeen = it })
                                    }
                                }
                            }
                        }
                        
                        SettingsSubScreen.NOTIFICATIONS -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("خيارات التنبيهات", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("تنبيهات الرسائل الفردية", fontWeight = FontWeight.Medium)
                                            Text("إصدار صوت واهتزاز للمحادثات الخاصة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = notifyMessage, onCheckedChange = { notifyMessage = it })
                                    }
                                    
                                    HorizontalDivider()
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("تنبيهات المجموعات", fontWeight = FontWeight.Medium)
                                            Text("تلقي إشعارات لمحادثات المجموعة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = notifyGroup, onCheckedChange = { notifyGroup = it })
                                    }
                                }
                            }
                        }
                        
                        SettingsSubScreen.ACCOUNT -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("إدارة الأمان", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                                    
                                    OutlinedButton(
                                        onClick = onChangePassword,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تغيير كلمة المرور")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}
