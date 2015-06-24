make tmote
if [ "$?" -ne "0" ]; then
   echo "COMPILE ERROR!"
   exit 1
fi
echo "Programming Node 0"
make tmote reinstall,5 bsl,/dev/tty.usbserial-M4A7N5TR
echo "Programming Node 1"
make tmote reinstall,6 bsl,/dev/tty.usbserial-XBS3IMZB
echo "Programming Node 2"
make tmote reinstall,7 bsl,/dev/tty.usbserial-XBS3IO7G
echo "Programming Node 3"
make tmote reinstall,8 bsl,/dev/tty.usbserial-XBS3ITLT
echo "Programming Node 4"
make tmote reinstall,9 bsl,/dev/tty.usbserial-XBS3IN64
echo "Finished."
