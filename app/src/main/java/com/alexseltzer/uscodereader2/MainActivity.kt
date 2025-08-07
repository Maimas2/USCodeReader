// Oops! All one file!
// MIT License

package com.alexseltzer.uscodereader2

import android.os.Bundle
import android.util.Xml
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.alexseltzer.uscodereader2.ui.theme.USCodeReader2Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.system.measureTimeMillis

val titles = arrayOf("General Provisions", "The Congress", "The President", "Flag and Seal, Seat of Government, and the States", "Government Organization and Employees", "Domestic Security", "Agriculture", "Aliens and Nationality", "Arbitration", "Armed Forces", "Bankruptcy", "Banks and Banking", "Census", "Coast Guard", "Commerce and Trade", "Conservation", "Copyrights", "Crimes and Criminal Procedure", "Customs Duties", "Education", "Food and Drugs", "Foreign Relations and Intercourse", "Highways", "Hospitals and Asylums", "Indians", "Internal Revenue Code", "Intoxicating Liquors", "Judiciary and Judicial Procedure", "Labor", "Mineral Lands and Mining", "Money and Finance", "National Guard", "Navigation and Navigable Waters", "Crime Control and Law Enforcement", "Patents", "Patriotic and National Observances, Ceremonies, and Organizations", "Pay and Allowances of the Uniformed Services", "Veterans' Benefits", "Postal Service", "Public Buildings, Property, and Works", "Public Contracts", "The Public Health and Welfare", "Public Lands", "Public Printing and Documents", "Railroads", "Shipping", "Telecommunications", "Territories and Insular Possessions", "Transportation", "War and National Defense", "National and Commercial Space Programs", "Voting and Elections", "[Currently Unused]", "National Park Service and Related Programs")

var chapters: ArrayList<ArrayList<ArrayList<String>>> = ArrayList()
var currentChapter: Int = 38839
var indentationLevel: Int = 0

var chapterNumbers: ArrayList<ArrayList<String>> = ArrayList()
var chapterTitles: ArrayList<ArrayList<String>> = ArrayList()
var subchapterSections: ArrayList<ArrayList<String>> = ArrayList()
var titleNames: ArrayList<String> = ArrayList()

val inlineTags: Array<String> = arrayOf("i", "ref", "date", "b")

var chpIdx: Int = -1

var time: Long = -1

var currentActivity: MainActivity = MainActivity()

var errorText: String = ""

lateinit var chapterLazyListState: LazyListState

@Serializable
data class TitleClass(val num: Int)

@Serializable
data class ChapterClass(val title: Int, var num: Int)

fun appendNum(text: String) {
    if(chapters[chpIdx][currentChapter][chapters[chpIdx][currentChapter].size - 1].matches("\\d(\\(.\\) *)*".toRegex())) {
        chapters[chpIdx][currentChapter][chapters[chpIdx][currentChapter].size - 1] += " $text"
    //} else if(chapters[currentTitleIt][currentChapter][chapters[currentTitleIt][currentChapter].size - 1].trim().endsWith("—")) {
    //    chapters[currentTitleIt][currentChapter][chapters[currentTitleIt][currentChapter].size - 1] += text
    } else {
        chapters[chpIdx][currentChapter].add("")
        chapters[chpIdx][currentChapter][chapters[chpIdx][currentChapter].size - 1] = indentationLevel.toString() + text
    }
}

fun appendText(text: String) {
    if (text.trim() != "") {
        if(chapters[chpIdx][currentChapter][chapters[chpIdx][currentChapter].size - 1].trim() == "") {
            chapters[chpIdx][currentChapter][chapters[chpIdx][currentChapter].size - 1] += indentationLevel.toString()
        }
        chapters[chpIdx][currentChapter][chapters[chpIdx][currentChapter].size - 1] += " ${
            text.replace(
                "\\s+".toRegex(),
                " "
            ).trim()
        }"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentActivity = this

        var iii: Int = 0
        while(++iii < 55) {
            chapters.add(ArrayList(0))
        }

        time = measureTimeMillis {
            try {
                for(i in 1..54) {
                    chapterNumbers.add(ArrayList(0))
                    subchapterSections.add(ArrayList())
                    if(i == 53) {
                        titleNames.add("Title 53")
                        chapterTitles.add(ArrayList())
                        continue
                    }

                    val fi = (assets.open("usc" + (if(i < 10) "0" else "") + i.toString() + "/title-info.txt"))
                    fi.bufferedReader().forEachLine { it ->
                        if(it.startsWith("FullTitle: ")) {
                            titleNames.add(it.substring(11))
                        } else {
                            chapterNumbers[i-1].add(it)
                            chapters[i-1].add(ArrayList())
                        }
                    }

                    chapterTitles.add(ArrayList())
                    val fii = (assets.open("usc" + (if(i < 10) "0" else "") + i.toString() + "/chapter-names.txt"))
                    fii.bufferedReader().forEachLine { it ->
                        if(it.trim().isNotEmpty()) {
                            chapterTitles[i-1].add(it)
                            chapters[i-1].add(ArrayList())
                        }
                    }


                    subchapterSections.add(ArrayList())
                    val fiii = (assets.open("usc" + (if(i < 10) "0" else "") + i.toString() + "/subchapters.txt"))
                    var lineNum: Int = 0
                    fiii.bufferedReader().forEachLine { it ->
                        if(lineNum > 0) {
                            val tkll = it.replace(".", "").replace("§", "").replace(",", "").split(" ")
                            if(lineNum % 2 == 1) {
                                subchapterSections[i-1].add("§ " + tkll.first().trim())
                            } else {
                                if(it.isNotEmpty()) {
                                    subchapterSections[i-1][subchapterSections[i-1].size-1] += " to " + tkll.last().trim()
                                } else {
                                    subchapterSections[i-1][subchapterSections[i-1].size-1] = ""
                                }
                            }
                        }
                        lineNum++
                    }
                }
                errorText = chapters[2].size.toString() + " " + chapterTitles[2].size.toString() + " " + chapterNumbers[2].size.toString() + " rrr"
            } catch(e: Exception) {
                throw e
            }
        }



        enableEdgeToEdge()
        setContent {
            val realDrawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope: CoroutineScope = rememberCoroutineScope()

            val navController = rememberNavController()

            chapterLazyListState = rememberLazyListState()

            USCodeReader2Theme {
                SharedTransitionLayout {
                    NavHost(
                        navController = navController, startDestination = "HelpMe",
//                        popExitTransition = {
//                            scaleOut(
//                                targetScale = 0.9f,
//                                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f)
//                            )
//                        },
//                        popEnterTransition = {
//                            EnterTransition.None
//                        }
                    ) {
                        composable("HelpMe") {
                            MainMenuThing(realDrawerState, scope, navController, this@SharedTransitionLayout, this@composable)
                        }
                        composable("I want to kill myself") {
                            Text("This is why you have no friends")
                        }
                        composable<TitleClass> { backStackEntry ->
                            ReadTitleScreen(backStackEntry.toRoute<TitleClass>().num, realDrawerState, scope, navController, this@SharedTransitionLayout, this@composable)
                        }
                        composable<ChapterClass> { backStackEntry ->
                            //Text(backStackEntry.toRoute<TitleClass>().num.toString())
                            ReadChapterScreen(backStackEntry.toRoute<ChapterClass>().title, backStackEntry.toRoute<ChapterClass>().num, realDrawerState, scope, navController, this@SharedTransitionLayout, this@composable)
                        }
                        composable("AboutUSCode") { backStackEntry ->
                            MainMenuBarFuncThing({
                                Scaffold(
                                    topBar = {
                                        CenterAlignedTopAppBar(
                                            title = { Text(text = "About the US Code", color = MaterialTheme.colorScheme.onPrimaryContainer) },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
                                            navigationIcon = {
                                                IconButton(onClick = {
                                                    scope.launch {
                                                        if (realDrawerState.isClosed) {
                                                            realDrawerState.open()
                                                        } else {
                                                            realDrawerState.close()
                                                        }
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Menu, contentDescription = "Menu");
                                                }
                                            },
                                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    },
                                    content = { innerPadding ->
                                        val m = Modifier.padding(10.dp).padding(horizontal = 10.dp)
                                        Column(modifier = Modifier.padding(innerPadding)) {
                                            Text("Each of the statutes passed by the Congress are compiled into the \"United States Statutes at Large,\" in which they are ordered chronologically by date passed with no respect to topic or content.", modifier = m)
                                            Text("The US Law Revision Counsel takes select statutes and groups them by topic into the 53 titles of the United States Code. While the statutes are left as original as possible, some formatting requires small changes to the bills; for example, 'the date of this bill's passage' may be replaced by the actual date.", modifier = m)
                                            Text("Not every statute is included in the US Code. Private statutes, which only apply to a select group of citizens as opposed to everyone; temporary statutes, which sunset after a specific time; and budget bills are excluded from the official code.", modifier = m)
                                            Text("Since the titles are not the direct product of the Congress, they generally do not have legal force and are for research only. \"Statutes at Large\" is the primary and official source. However, the Congress may repass a full, edited title as 'positive law,' giving the edited version legal force.", modifier = m)
                                        }
                                    },
                                )
                            }, realDrawerState, scope, navController)
                        }
                        composable("AboutApp") { backStackEntry ->
                            MainMenuBarFuncThing({
                                Scaffold(
                                    topBar = {
                                        CenterAlignedTopAppBar(
                                            title = { Text(text = "About this App", color = MaterialTheme.colorScheme.onPrimaryContainer) },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
                                            navigationIcon = {
                                                IconButton(onClick = {
                                                    scope.launch {
                                                        if (realDrawerState.isClosed) {
                                                            realDrawerState.open()
                                                        } else {
                                                            realDrawerState.close()
                                                        }
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Menu, contentDescription = "Menu");
                                                }
                                            },
                                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    },
                                    content = { innerPadding ->
                                        val m = Modifier.padding(10.dp).padding(horizontal = 10.dp)
                                        Column(modifier = Modifier.padding(innerPadding)) {
                                            Text("This app is intended to provide a more user-friendly and modern UI to read the United States Code. I was unsatisfied with the current official website at uscode.house.gov, leading to this.", modifier = m)

                                            Spacer(modifier = Modifier.padding(7.dp))
                                            HorizontalDivider()
                                            Spacer(modifier = Modifier.padding(7.dp))

                                            Text("All text presented in this app is attempted to be as faithful to the original US Code as possible, although there may be formatting errors in loading. I have not changed any legal text.", modifier = m)
                                            Text("All text comes from the XML version of the US Code hosted at uscode.house.gov, with excess and unnecessary tags excluded. By doing so, I more than halved the amount of storage taken required. I also split each title into chapters for faster loading. The bash files used to set everything up are included in the Github. See autogen.sh.", modifier = m)
                                        }
                                    },
                                )
                            }, realDrawerState, scope, navController)
                        }
                        composable("Legal") { backStackEntry ->
                            MainMenuBarFuncThing({
                                Scaffold(
                                    topBar = {
                                        CenterAlignedTopAppBar(
                                            title = { Text(text = "Legal Disclaimer", color = MaterialTheme.colorScheme.onPrimaryContainer) },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
                                            navigationIcon = {
                                                IconButton(onClick = {
                                                    scope.launch {
                                                        if (realDrawerState.isClosed) {
                                                            realDrawerState.open()
                                                        } else {
                                                            realDrawerState.close()
                                                        }
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Menu, contentDescription = "Menu");
                                                }
                                            },
                                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    },
                                    content = { innerPadding ->
                                        val m = Modifier.padding(10.dp).padding(horizontal = 10.dp)
                                        Column(modifier = Modifier.padding(innerPadding)) {
                                            Text("This app is not legal advice and should not be interpreted as such. I am in no way responsible for the use or misuse of this app or any of its contents.", modifier = m)
                                            Spacer(modifier = Modifier.padding(7.dp))
                                            HorizontalDivider()
                                            Spacer(modifier = Modifier.padding(7.dp))
                                            Text("This app is licensed under the MIT License:", modifier = m)
                                            Text("Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n" +
                                                    "\n" +
                                                    "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n" +
                                                    "\n" +
                                                    "THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.", modifier = m)
                                            Spacer(modifier = Modifier.padding(15.dp))
                                        }
                                    },
                                )
                            }, realDrawerState, scope, navController)
                        }
                    }
                }
            }
        }
    }
}

fun loadTitleFile(f: InputStream, titleId: Int, chapterId: Int) {
    val parser = Xml.newPullParser()

    chpIdx = titleId

    parser.setInput(InputStreamReader(f))

    currentChapter = chapterId
    indentationLevel = 0

    var tag: String? = ""
    var text: String = ""
    var event = parser.eventType

    var isInContent: Int = 0
    var isInNotes: Boolean = false
    var isInQuotes: Boolean = false

    while (event != XmlPullParser.END_DOCUMENT) {
        tag = parser.name
        when (event) {
            XmlPullParser.START_TAG -> {
                val cl: String? = parser.getAttributeValue(null, "class")
                if (cl != null) {
                    if      (cl.contains("indent0")) indentationLevel = 0;
                    else if (cl.contains("indent1")) indentationLevel = 1;
                    else if (cl.contains("indent2")) indentationLevel = 2;
                    else if (cl.contains("indent3")) indentationLevel = 3;
                    else if (cl.contains("indent4")) indentationLevel = 4;
                }
                if (tag == "content" || tag == "chapeau") {
                    if (chapters[chpIdx][currentChapter].isNotEmpty() &&
                        !chapters[chpIdx][currentChapter][chapters[chpIdx][currentChapter].size - 1].endsWith(")")) {
                        chapters[chpIdx][currentChapter].add("")
                    }
                    isInContent++
                }
                if (inlineTags.contains(tag) && isInContent > 0) {
                    appendText(text)
                }
                if (tag == "notes") {
                    isInNotes = true
                }
                if (tag == "quotedContent") isInQuotes = true
            }

            XmlPullParser.TEXT -> {
                text = parser.text
                if (tag == "quotedContent" || isInNotes) text = "";
            }

            XmlPullParser.END_TAG -> {
                if (text != "" && currentChapter >= 0 && !isInQuotes) {

                    if (chapters[chpIdx][currentChapter].isEmpty()) chapters[chpIdx][currentChapter].add("")
                    if (tag == "num") {
                        appendNum(text)
                        text = ""
                    }
                    if (tag == "heading") {
                        appendText(text)
                        text = ""
                    }
                    if (inlineTags.contains(tag) && isInContent > 0) { // Tags that can be put in the middle of a paragraph for citing or such
                        appendText(text)
                    }
                    if (isInContent > 0 && tag == "p") {
                        appendText(text)
                        text = "";
                    }
                    if ((tag == "content" || tag == "chapeau") && (!isInNotes)) { // They're synonyms or something ???????
                        appendText(text)
                    }
                }

                if (tag == "content" || tag == "chapeau") isInContent--
                if (tag == "notes") isInNotes = false
                if (tag == "quotedContent") isInQuotes = false
            }
        }
        event = parser.next()
    }
    while(chapters[chpIdx][currentChapter].isNotEmpty() && chapters[chpIdx][currentChapter][0].trim().isEmpty()) {
        chapters[chpIdx][currentChapter].removeAt(0)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainMenuThing(realDrawerState: DrawerState, scope: CoroutineScope, navController: NavController, sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope) {
    MainMenuBarFuncThing({
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "US Code", color = MaterialTheme.colorScheme.onPrimaryContainer) },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (realDrawerState.isClosed) {
                                    realDrawerState.open()
                                } else {
                                    realDrawerState.close()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu");
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            content = { innerPadding ->
                LazyColumn(modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
                    .padding(innerPadding)) {
                    items(count = titles.size) { i ->
                        MainCard(i, realDrawerState, scope, navController, sharedTransitionScope, animatedVisibilityScope)
                    }
                }
            },
        )
    }, realDrawerState, scope, navController)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainCard(i: Int, realDrawerState: DrawerState, scope: CoroutineScope, navController: NavController, sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope) {
    with(sharedTransitionScope) {
        Card(
            modifier = Modifier
                .padding(10.dp)
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center)
                .padding(vertical = 10.dp)
                .defaultMinSize(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            onClick = {
                scope.launch {
                    realDrawerState.close()
                    navController.navigate(TitleClass(i))
                }
            }
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = "Title ${i + 1}: ${titles[i]}",
                    modifier = Modifier
                        .sharedElement(
                            sharedTransitionScope.rememberSharedContentState(key = "TitleTitle${i}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
//                Text(
//                    text = titles[i],
//                    modifier = Modifier.fillMaxWidth(),
//                    textAlign = TextAlign.Center,
//                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TitleCard(titleNum: Int, chapterNum: Int, realDrawerState: DrawerState, scope: CoroutineScope, navController: NavController, sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(10.dp)
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
            .padding(vertical = 10.dp)
            .padding(top = if(chapterNum == 0) 20.dp else 0.dp)
            .defaultMinSize(30.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            with(sharedTransitionScope) {
                Text(
                    //text = "Chapter $chapterNum",
                    text = chapterNumbers[titleNum][chapterNum],
                    modifier = Modifier
                        .sharedElement(
                            sharedTransitionScope.rememberSharedContentState(key = "ChapterTitle${titleNum} $chapterNum"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = chapterTitles[titleNum][chapterNum],
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if(subchapterSections[titleNum][chapterNum].isNotEmpty()) {
                    Text(
                        text = subchapterSections[titleNum][chapterNum],
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

fun isImportant(s: String): Boolean {
    return s.startsWith("§") || s.startsWith("SUBCHAPTER")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ReadTitleScreen(titleNum: Int, realDrawerState: DrawerState, scope: CoroutineScope, navController: NavController, sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    with(sharedTransitionScope) {
                        Text(
                            "Title ${titleNum + 1}: ${titles[titleNum]}",
//                            titleNames[titleNum],
                            modifier = Modifier.sharedElement(sharedTransitionScope.rememberSharedContentState(key = "TitleTitle${titleNum}"), animatedVisibilityScope = animatedVisibilityScope),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } },
                //title = { Text("Title ${chapterNum + 1}: ${titles[chapterNum]}") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    },
                        colors = IconButtonDefaults.iconButtonColors()) {
                        Icon(Icons.Outlined.Close, contentDescription = "null")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        content = { innerPadding ->
            LazyColumn(modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)) {

                items(chapterNumbers[titleNum].size-1) { ii ->
                    // chapters[titleNum][ii][0]
                    TitleCard(titleNum, ii, realDrawerState, scope, navController, sharedTransitionScope, animatedVisibilityScope, {
                        scope.launch {
                            if(chapters[titleNum][ii].isEmpty()) {
                                val n = "usc" + (if(titleNum < 9) "0" else "") + (titleNum+1).toString() + "/usc-00" + (if(titleNum < 99) "0" else "") + (if(ii < 9) "0" else "") + (ii+1).toString() + ".xml"

                                loadTitleFile(currentActivity.assets.open(n), titleNum, ii)
                            }
                            realDrawerState.close()
                            navController.navigate(ChapterClass(titleNum, ii))
                            chapterLazyListState.scrollToItem(0)
                        }
                    })
                }
            }
        }
    )
}

@Composable
fun MainMenuBarFuncThing(interior: @Composable () -> Unit, realDrawerState: DrawerState, scope: CoroutineScope, navController: NavController) {
    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet() {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("US Code", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationDrawerItem(
                        label = { Text("Titles") },
                        selected = false,
                        icon = { Icon(Icons.Outlined.Home, contentDescription = "null") },
                        onClick = {
                            scope.launch {
                                realDrawerState.close()
                            }
                            scope.launch {
                                navController.currentDestination?.equals("HelpMe")?.let {
                                    if(!it) navController.navigate("HelpMe") {
                                        popUpTo("HelpMe") {
                                            inclusive = true
                                        }
                                    }
                                }
                            }
                        }
                    )
//                    NavigationDrawerItem(
//                        //label = { Text(time.toString()) },
//                        label = { Text(errorText) },
//                        selected = false,
//                        icon = { Icon(Icons.Outlined.Settings, contentDescription = "null") },
//                        onClick = { /* Handle click */ }
//                    )
                    NavigationDrawerItem(
                        label = { Text("About US Code") },
                        selected = false,
                        icon = { Icon(Icons.Outlined.Info, contentDescription = "null") },
                        onClick = {
                            scope.launch {
                                realDrawerState.close()
                            }
                            scope.launch {
                                navController.currentDestination?.equals("AboutUSCode")?.let { if(!it) navController.navigate("AboutUSCode") }
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("About this App") },
                        selected = false,
                        icon = { Icon(Icons.Outlined.Info, contentDescription = "null") },
                        onClick = {
                            scope.launch {
                                realDrawerState.close()
                            }
                            scope.launch {
                                navController.currentDestination?.equals("AboutApp")?.let { if(!it) navController.navigate("AboutApp") }
                            }
                        },
                    )
                    NavigationDrawerItem(
                        label = { Text("Legal Disclaimer") },
                        selected = false,
                        onClick = {
                            scope.launch {
                                realDrawerState.close()
                            }
                            scope.launch {
                                navController.currentDestination?.equals("Legal")?.let { if(!it) navController.navigate("Legal") }
                            }
                        },
                    )
                }
            }
        },
        drawerState = realDrawerState
    ) {
        interior()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ReadChapterScreen(titleNum: Int, chapterNum: Int, realDrawerState: DrawerState, scope: CoroutineScope, navController: NavController, sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope) {
    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet() {
                LazyColumn {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Title ${titleNum + 1}, ${chapterNumbers[titleNum][chapterNum]}",
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(), style = MaterialTheme.typography.titleLarge
                        )
                        HorizontalDivider()
                    }
                    items(chapters[titleNum][chapterNum].size) { i ->
                        if (chapters[titleNum][chapterNum][i].isNotEmpty() && chapters[titleNum][chapterNum][i].substring(
                                1
                            ).startsWith("§")
                        ) {
                            NavigationDrawerItem(
                                label = { Text(chapters[titleNum][chapterNum][i].substring(3)) },
                                selected = false,
                                onClick = {
                                    scope.launch {
                                        chapterLazyListState.animateScrollToItem(index = i)
                                    }
                                    scope.launch {
                                        realDrawerState.close()
                                    }
                                },
                                shape = RectangleShape
                            )
                        }
                    }
                }
            }
        },
        drawerState = realDrawerState
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        with(sharedTransitionScope) {
                            Text(
                                //"Title ${titleNum + 1}, ${chapterNumbers[titleNum][chapterNum]}",
                                text = chapterNumbers[titleNum][chapterNum],
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.sharedElement(sharedTransitionScope.rememberSharedContentState(key = "ChapterTitle${titleNum} $chapterNum"), animatedVisibilityScope = animatedVisibilityScope)
                            )
                        }
                    },
                    //title = { Text("Title ${chapterNum + 1}: ${titles[chapterNum]}") },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Outlined.Close, contentDescription = "null")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                realDrawerState.open()
                            }
                        }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "null")
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                )
            },
            content = { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxWidth(), state = chapterLazyListState
                ) {
                    item {
//                        Text(
//                            "${titleNum+1} U.S.C. § ${chapterNum+1}",
//                            fontWeight = FontWeight.Bold,
//                            fontSize = 18.sp,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(15.dp)
//                                .padding(top = 20.dp),
//                            textAlign = TextAlign.Center,
//                        )
                        Text(
                            chapterTitles[titleNum][chapterNum],
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(15.dp),
                            textAlign = TextAlign.Center,
                        )
                    }

                    items(chapters[titleNum][chapterNum].size) { ii ->
                        if(ii > 0) {
                            if (chapters[titleNum][chapterNum][ii].trim().length > 2) {
                                var m: Modifier = Modifier.fillMaxWidth()
                                val indLev: Int = chapters[titleNum][chapterNum][ii].substring(0, 1).toInt()

                                if(isImportant(chapters[titleNum][chapterNum][ii].substring(1).trim())) {
                                    m = m.padding(15.dp)
                                    Text(
                                        chapters[titleNum][chapterNum][ii].substring(1).trim(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if(chapters[titleNum][chapterNum][ii].substring(1).trim().startsWith("SUBCHAPTER")) 25.sp else 20.sp,
                                        modifier = m,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    m = m
                                        .padding(5.dp)
                                        .padding(start = 5.dp + 15.dp * indLev)
                                    Text(
                                        chapters[titleNum][chapterNum][ii].substring(1).trim(),
                                        modifier = m
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.primary) {
        Text(
            text = "Hello, my name is $name, and I hate my life!",
            modifier = modifier,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}