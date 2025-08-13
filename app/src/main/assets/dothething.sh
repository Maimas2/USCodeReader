mv $1 usc.xml

echo "    Splitting" $1 "..."

xml_split -n 5 -c chapter usc.xml

echo "FullTitle:" `xml_grep --text_only 'uscDoc/meta/dc:title' usc-00000.xml` > title-info.txt

echo > chapter-names.txt
echo > subchapters.txt

echo "    Extracting names from" $1 "..."

rm usc-00000.xml

for f in `find usc-*.xml`
do

echo `xml_grep --text_only 'chapter/num' $f | head -c-4` >> title-info.txt
echo `xml_grep --text_only 'chapter/heading' $f` >> chapter-names.txt

echo `xml_grep --text_only 'num' $f | grep -E '^(§)' | head -n1` >> subchapters.txt
echo `xml_grep --text_only 'num' $f | grep -E '^(§)' | tail -n1` >> subchapters.txt

done

mv usc.xml $1
#rm usc.xml
