package com.nevercrynow.ioshub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nevercrynow.ioshub.data.LinkRouter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevercrynow.ioshub.data.AppState
import com.nevercrynow.ioshub.data.Category
import com.nevercrynow.ioshub.ui.Brand
import com.nevercrynow.ioshub.ui.CategoryScreen
import com.nevercrynow.ioshub.ui.DetailScreen
import com.nevercrynow.ioshub.ui.DiscoverScreen
import com.nevercrynow.ioshub.ui.GlassBar
import com.nevercrynow.ioshub.ui.FavoritesScreen
import com.nevercrynow.ioshub.ui.HomeScreen
import com.nevercrynow.ioshub.ui.NewsScreen
import com.nevercrynow.ioshub.ui.IosHubTheme
import com.nevercrynow.ioshub.ui.LaunchOverlay
import com.nevercrynow.ioshub.ui.ProfileScreen
import com.nevercrynow.ioshub.ui.ResourcesScreen
import com.nevercrynow.ioshub.ui.SearchScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LinkRouter.attach(applicationContext)
        setContent {
            IosHubTheme {
                // 启动动画结束后才卸载，首页在它下面已经开始渲染，切换无白屏
                var launching by remember { mutableStateOf(true) }
                Box(Modifier.fillMaxSize()) {
                    HubApp()
                    if (launching) {
                        LaunchOverlay(onFinished = { launching = false })
                    }
                }
            }
        }
    }
}

/** 极简导航：一个栈 + 四个根 Tab。没有引入 navigation-compose，减少依赖。 */
sealed interface Screen {
    data object Home : Screen
    data object Resources : Screen
    data object Favorites : Screen
    data object Profile : Screen
    data object Search : Screen
    data object Discover : Screen
    data object News : Screen
    data class CategoryDetail(val category: Category) : Screen
    data class Detail(val id: String) : Screen
}

private data class Tab(val title: String, val icon: ImageVector, val screen: Screen)

private val Tabs = listOf(
    Tab("首页", Icons.Filled.Home, Screen.Home),
    Tab("发现", Icons.Filled.Search, Screen.Discover),
    Tab("资源", Icons.AutoMirrored.Filled.List, Screen.Resources),
    Tab("收藏", Icons.Filled.Favorite, Screen.Favorites),
    Tab("我的", Icons.Filled.Person, Screen.Profile),
)

@Composable
private fun HubApp() {
    val context = LocalContext.current
    val state = remember { AppState(context.applicationContext) }
    LaunchedEffect(Unit) { state.bootstrap() }
    // 启动时静默探一次线路：直连不通就自动切镜像，之后打开链接零等待
    LaunchedEffect(Unit) { LinkRouter.autoDetect() }

    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }
    val current = stack.last()
    val selectedIndex = Tabs.indexOfFirst { it.screen == stack.first() }.coerceAtLeast(0)

    BackHandler(enabled = stack.size > 1) {
        stack = stack.dropLast(1)
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // 悬浮的液态玻璃导航条：不贴底，四周留白，让下面的光斑透上来
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 12.dp),
            ) {
                GlassBar(corner = 26.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(62.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Tabs.forEachIndexed { index, tab ->
                            val selected = selectedIndex == index
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { stack = listOf(tab.screen) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (selected) Brand else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp),
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    tab.title,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Brand else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(3.dp))
                                Box(
                                    Modifier
                                        .size(3.dp)
                                        .background(
                                            if (selected) Brand else Color.Transparent,
                                            CircleShape,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val screen = current) {
                Screen.Home -> HomeScreen(
                    state = state,
                    onOpenDetail = { stack = stack + Screen.Detail(it.id) },
                    onOpenCategory = { stack = stack + Screen.CategoryDetail(it) },
                    onOpenSearch = { stack = stack + Screen.Search },
                    onOpenNews = { stack = stack + Screen.News },
                )

                Screen.Discover -> DiscoverScreen(state = state)

                Screen.News -> NewsScreen(
                    state = state,
                    onBack = { stack = stack.dropLast(1) },
                )

                Screen.Resources -> ResourcesScreen(
                    state = state,
                    onCategory = { stack = stack + Screen.CategoryDetail(it) },
                    onSearch = { stack = stack + Screen.Search },
                )

                Screen.Favorites -> FavoritesScreen(
                    state = state,
                    onDetail = { stack = stack + Screen.Detail(it.id) },
                )

                Screen.Profile -> ProfileScreen(state = state)

                Screen.Search -> SearchScreen(
                    state = state,
                    onBack = { stack = stack.dropLast(1) },
                    onDetail = { stack = stack + Screen.Detail(it.id) },
                )

                is Screen.CategoryDetail -> CategoryScreen(
                    state = state,
                    category = screen.category,
                    onBack = { stack = stack.dropLast(1) },
                    onDetail = { stack = stack + Screen.Detail(it.id) },
                )

                is Screen.Detail -> DetailScreen(
                    state = state,
                    itemId = screen.id,
                    onBack = { stack = stack.dropLast(1) },
                )
            }
        }
    }
}
