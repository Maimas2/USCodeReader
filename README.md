# United State Code Reader

This is an app designed to provide a better-looking and more user-friendly interface for reading the United States Code, the indexed legal code of the United State of America. The Law Revision Counsel (LRC) serves the Code at [uscode.house.gov](https://uscode.house.gov/), although the website is (in my arrogant opinion) not beautiful.

All text of the US Code in this app is taken from the LRC's XML format of the Code, hosted at the above link. The raw XML files bundled in the app are **not** original: they have been split into chapters and filtered to remove unnecessary tags. Doing so improves loading times and reduces the app's size. Again, the actual text is not changed, bar any small formatting errors.

All tools to build the XML files are included in the source. To turn the files from [uscode.house.gov](https://uscode.house.gov/) into those included in this app, download the full XML .zip file, extract them to a folder, and copy `autodo.sh`, `cleanusc.sh`, and `dosomething.sh` into the same folder. Run `bash autodo.sh`, and it do everything required. The build process depends on `xml_grep`, `xml_split`, and `xmlstarlet`.

Inquiries, comments, or literally anything at all can be sent to ASeltz156@gmail.com

This app is licensed under the [MIT License](https://mit-license.org/).
