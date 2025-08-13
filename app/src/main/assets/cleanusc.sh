xml_grep --exclude "notes" usc.xml > usc-t.xml
xml_grep --exclude "note" usc-t.xml > usc-tt.xml
xml_grep --exclude "toc" usc-tt.xml > usc.xml
xmlstarlet ed -d '//@id' usc.xml > usc-t.xml
xmlstarlet ed -d '//@style' usc-t.xml > usc.xml
xmlstarlet ed -d '//@identifier' usc.xml > usc-t.xml
xmlstarlet ed -d '//@href' usc-t.xml > usc.xml
xmlstarlet ed -d '//@value' usc.xml > usc-t.xml
xmlstarlet ed -d '//@status' usc-t.xml > usc-tt.xml

xmlstarlet fo -n usc-tt.xml > usc.xml

rm usc-t.xml usc-tt.xml
