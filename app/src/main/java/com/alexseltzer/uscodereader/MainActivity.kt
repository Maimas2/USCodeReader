// Oops! All one file!
// MIT License

package com.alexseltzer.uscodereader

import android.os.Build
import android.os.Bundle
import android.util.Xml
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.substring
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.alexseltzer.uscodereader.ui.theme.USCodeReaderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale
import kotlin.system.measureTimeMillis

val titles = arrayOf("General Provisions", "The Congress", "The President", "Flag and Seal, Seat of Government, and the States", "Government Organization and Employees", "Domestic Security", "Agriculture", "Aliens and Nationality", "Arbitration", "Armed Forces", "Bankruptcy", "Banks and Banking", "Census", "Coast Guard", "Commerce and Trade", "Conservation", "Copyrights", "Crimes and Criminal Procedure", "Customs Duties", "Education", "Food and Drugs", "Foreign Relations and Intercourse", "Highways", "Hospitals and Asylums", "Indians", "Internal Revenue Code", "Intoxicating Liquors", "Judiciary and Judicial Procedure", "Labor", "Mineral Lands and Mining", "Money and Finance", "National Guard", "Navigation and Navigable Waters", "Crime Control and Law Enforcement", "Patents", "Patriotic and National Observances, Ceremonies, and Organizations", "Pay and Allowances of the Uniformed Services", "Veterans' Benefits", "Postal Service", "Public Buildings, Property, and Works", "Public Contracts", "The Public Health and Welfare", "Public Lands", "Public Printing and Documents", "Railroads", "Shipping", "Telecommunications", "Territories and Insular Possessions", "Transportation", "War and National Defense", "National and Commercial Space Programs", "Voting and Elections", "[Currently Unused]", "National Park Service and Related Programs")

var chapters: ArrayList<ArrayList<ArrayList<String>>> = ArrayList()
var indentationLevel: Int = 0

var constitutionText: ArrayList<String> = ArrayList()

var chapterNumbers: ArrayList<ArrayList<String>> = ArrayList()
var chapterTitles: ArrayList<ArrayList<String>> = ArrayList()
var subchapterSections: ArrayList<ArrayList<String>> = ArrayList()
var titleNames: ArrayList<String> = ArrayList()

val inlineTags: Array<String> = arrayOf("i", "ref", "date", "b")

var time: Long = -1

var currentActivity: MainActivity = MainActivity()

var errorText: String = ""

lateinit var chapterLazyListState: LazyListState

@Serializable
data class TitleClass(val num: Int)

@Serializable
data class ChapterClass(val title: Int, var num: Int)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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

        constitutionText = loadTitleFile(currentActivity.assets.open("constitution.xml"))

        enableEdgeToEdge()
        setContent {
            val realDrawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope: CoroutineScope = rememberCoroutineScope()

            val navController = rememberNavController()

            chapterLazyListState = rememberLazyListState()

            USCodeReaderTheme {
                SharedTransitionLayout {
                    NavHost(
                        navController = navController, startDestination = "TheSigma"
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
                        composable("TheSigma") {
                            MainMenuThing(realDrawerState, scope, navController, this@SharedTransitionLayout, this@composable)
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

                                            Text(buildAnnotatedString {
                                                append("The text for the Constitution comes from the version hosted at ")
                                                withLink(
                                                    LinkAnnotation.Url(
                                                        "https://constitution.congress.gov/",
                                                        TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                                                    )
                                                ) {
                                                    append("https://constitution.congress.gov/")
                                                }
                                                append("; internally, I have adapted it into an XML form able to be loaded and displayed by the app.")
                                            }, modifier = m)

                                            Spacer(modifier = Modifier.padding(7.dp))
                                            HorizontalDivider()
                                            Spacer(modifier = Modifier.padding(7.dp))

                                            Text(buildAnnotatedString {
                                                append("Created by Alex Seltzer. Check out my website at ")
                                                withLink(
                                                    LinkAnnotation.Url(
                                                        "https://alex-seltzer.com/",
                                                        TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                                                    )
                                                ) {
                                                    append("https://alex-seltzer.com/")
                                                }
                                            }, modifier = m)

                                            Text(buildAnnotatedString {
                                                append("Source code hosted at ")
                                                withLink(
                                                    LinkAnnotation.Url(
                                                        "https://github.com/Maimas2/USCodeReader/",
                                                        TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                                                    )
                                                ) {
                                                    append("GitHub")
                                                }
                                            }, modifier = m)

                                            Text("Contact me for any reason at ASeltz156@gmail.com", modifier = m)

                                            Text("Version 1.0", modifier = m)
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
                        composable("USConstitution") { backStackEntry ->
                            ReadChapterScreen("US Constitution", "US Constitution", "", constitutionText, realDrawerState, scope, navController, this@SharedTransitionLayout, this@composable)
                        }
                    }
                }
            }
        }
    }
}

fun loadTitleFile(f: InputStream): ArrayList<String> {
    val parser = Xml.newPullParser()

    var toReturn: ArrayList<String> = ArrayList()

    parser.setInput(InputStreamReader(f))

    indentationLevel = 0

    fun appendText(text: String) {
        if (text.trim() != "") {
            if(toReturn[toReturn.size - 1].trim() == "") {
                toReturn[toReturn.size - 1] += indentationLevel.toString()
            }
            toReturn[toReturn.size - 1] += " ${
                text.replace(
                    "\\s+".toRegex(),
                    " "
                ).trim()
            }"
        }
    }
    fun appendNum(text: String) {
        if(toReturn[toReturn.size - 1].matches("\\d(\\(.\\) *)*".toRegex())) {
            toReturn[toReturn.size - 1] += " $text"
        } else {
            toReturn.add(indentationLevel.toString() + text)
        }
    }

    var tag: String?
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
                    if (toReturn.isNotEmpty() &&
                        !toReturn[toReturn.size - 1].endsWith(")")) {
                        toReturn.add("")
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
                if (text != "" && !isInQuotes) {
                    if (toReturn.isEmpty()) toReturn.add("")
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
    while(toReturn.isNotEmpty() && toReturn[0].trim().isEmpty()) {
        toReturn.removeAt(0)
    }
    return toReturn
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

fun isImportant(ss: String): Boolean {
    val s = ss.uppercase(Locale.ROOT);
    return s.startsWith("§") || s.startsWith("SUBCHAPTER") || s.startsWith("PART") || s.startsWith("PARAGRAPH") || s.startsWith("ARTICLE") || (s.startsWith("SECTION") && !s.startsWith("SECTIONS")) || s.startsWith("THE PREAMBLE") || s.endsWith("AMENDMENT");
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
            if(titleNum+1 == 53) { // The unused title
                Column(modifier = Modifier.padding(innerPadding).padding(5.dp)) {
                    Text("Title 53 is currently unused.")
                }
            } else {
                LazyColumn(modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)) {

                    items(chapterNumbers[titleNum].size-1) { ii ->
                        // chapters[titleNum][ii][0]
                        TitleCard(titleNum, ii, realDrawerState, scope, navController, sharedTransitionScope, animatedVisibilityScope, {
                            scope.launch {
                                if(chapters[titleNum][ii].isEmpty()) {
                                    val n = "usc" + (if(titleNum < 9) "0" else "") + (titleNum+1).toString() + "/usc-00" + (if(titleNum < 99) "0" else "") + (if(ii < 9) "0" else "") + (ii+1).toString() + ".xml"

                                    if(titleNum+1 != 53) {
                                        chapters[titleNum][ii] = loadTitleFile(currentActivity.assets.open(n))
                                    }
                                }
                                realDrawerState.close()
                                navController.navigate(ChapterClass(titleNum, ii))
                                chapterLazyListState.scrollToItem(0)
                            }
                        })
                    }
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
                                navController.currentDestination?.equals("TheSigma")?.let {
                                    if(!it) navController.navigate("TheSigma") {
                                        popUpTo("TheSigma") {
                                            inclusive = true
                                        }
                                        popUpTo("Legal") {
                                            inclusive = true
                                        }
                                        popUpTo("AboutApp") {
                                            inclusive = true
                                        }
                                        popUpTo("AboutUSCode") {
                                            inclusive = true
                                        }
                                    }
                                }
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("US Constitution") },
                        selected = false,
                        onClick = {
                            scope.launch {
                                realDrawerState.close()
                            }
                            scope.launch {
                                navController.currentDestination?.equals("USConstitution")?.let { if(!it) navController.navigate("USConstitution") {
                                    popUpTo("Legal") {
                                        inclusive = true
                                    }
                                    popUpTo("AboutApp") {
                                        inclusive = true
                                    }
                                    popUpTo("AboutUSCode") {
                                        inclusive = true
                                    }
                                } }
                            }
                        },
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
                                navController.currentDestination?.equals("AboutUSCode")?.let { if(!it) navController.navigate("AboutUSCode") {
                                    popUpTo("Legal") {
                                        inclusive = true
                                    }
                                    popUpTo("AboutApp") {
                                        inclusive = true
                                    }
                                    popUpTo("AboutUSCode") {
                                        inclusive = true
                                    }
                                } }
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
                                navController.currentDestination?.equals("AboutApp")?.let { if(!it) navController.navigate("AboutApp") {
                                    popUpTo("Legal") {
                                        inclusive = true
                                    }
                                    popUpTo("AboutApp") {
                                        inclusive = true
                                    }
                                    popUpTo("AboutUSCode") {
                                        inclusive = true
                                    }
                                } }
                            }
                        },
                    )
                    NavigationDrawerItem(
                        label = { Text("Legal") },
                        selected = false,
                        onClick = {
                            scope.launch {
                                realDrawerState.close()
                            }
                            scope.launch {
                                navController.currentDestination?.equals("Legal")?.let { if(!it) navController.navigate("Legal") {
                                    popUpTo("Legal") {
                                        inclusive = true
                                    }
                                    popUpTo("AboutApp") {
                                        inclusive = true
                                    }
                                    popUpTo("AboutUSCode") {
                                        inclusive = true
                                    }
                                } }
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
    ReadChapterScreen(chapterNumbers[titleNum][chapterNum], "Title ${titleNum + 1}, ${chapterNumbers[titleNum][chapterNum]}", chapterTitles[titleNum][chapterNum], chapters[titleNum][chapterNum], realDrawerState, scope, navController, sharedTransitionScope, animatedVisibilityScope)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ReadChapterScreen(title: String, drawerTitle: String, titleTitle: String, texts: ArrayList<String>, realDrawerState: DrawerState, scope: CoroutineScope, navController: NavController, sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope) {
    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet() {
                LazyColumn {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            //"Title ${titleNum + 1}, ${chapterNumbers[titleNum][chapterNum]}",
                            drawerTitle,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(), style = MaterialTheme.typography.titleLarge
                        )
                        HorizontalDivider()
                    }
                    items(texts.size) { i ->
                        if (texts[i].isNotEmpty() && isImportant(texts[i].substring(1))
                        ) {
                            NavigationDrawerItem(
                                label = { if(texts[i].startsWith("§")) Text(texts[i].substring(3)) else Text(texts[i].substring(1)) },
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
                                text = title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                //modifier = Modifier.sharedElement(sharedTransitionScope.rememberSharedContentState(key = "ChapterTitle${titleNum} $chapterNum"), animatedVisibilityScope = animatedVisibilityScope)
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
                        if(titleTitle.isNotEmpty()) {
                            Text(
                                titleTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(15.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    items(texts.size) { ii ->
                        if(ii > 0) {
                            if (texts[ii].trim().length > 2) {
                                var m: Modifier = Modifier.fillMaxWidth()
                                val indLev: Int = texts[ii].substring(0, 1).toInt()

                                if(isImportant(texts[ii].substring(1).trim())) {
                                    m = m.padding(15.dp)
                                    Text(
                                        if(texts[ii].matches("\\d.*".toRegex())) texts[ii].substring(1).trim() else texts[ii].trim(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        modifier = m,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    m = m
                                        .padding(5.dp)
                                        .padding(start = 5.dp + 15.dp * indLev)
                                    Text(
                                        texts[ii].substring(1).trim(),
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
