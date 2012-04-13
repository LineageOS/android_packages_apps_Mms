for i in emoji_*; do
pngcrush -brute $i ../drawable-hdpi-brute/$i; 
done;
