for f in `find usc*.xml`
do

echo "Doing" $f "..."

mkdir `echo $f | head -c5`
cp $f `echo $f | head -c5`
cp dothething.sh `echo $f | head -c5`
cp cleanusc.sh `echo $f | head -c5`

cd `echo $f | head -c5`

echo "  Cleaning" $f "..."

mv $f usc.xml
bash cleanusc.sh
mv usc.xml $f

echo "  Extracting" $f "..."

bash dothething.sh $f
rm dothething.sh
rm cleanusc.sh
rm $f
cd ..

done
