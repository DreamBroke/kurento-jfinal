echo "deb http://ubuntu.kurento.org xenial kms6" | sudo tee /etc/apt/sources.list.d/kurento.list
wget -O - http://ubuntu.kurento.org/kurento.gpg.key | sudo apt-key add -
sudo apt-get update
sudo apt-get -y install kurento-media-server-6.0
sudo apt-get -y install openh264-gst-plugins-bad-1.5
echo stunServerAddress=54.223.76.143 >> set_stun_script.txt
echo stunServerPort=3478 >> set_stun_script.txt
cat set_stun_script.txt | sudo tee -a /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
mkdir -m 777 video
sudo apt-get -y install automake autotools-dev fuse g++ git libcurl4-gnutls-dev libfuse-dev libssl-dev libxml2-dev make pkg-config
git clone https://github.com/s3fs-fuse/s3fs-fuse.git
cd s3fs-fuse
./autogen.sh
./configure
make
sudo make install
echo YOUR_AWS_ID:YOUR_AWS_KEY > /home/ubuntu/.passwd-s3fs
chmod 600 /home/ubuntu/.passwd-s3fs
s3fs wisonic-video-save /home/ubuntu/video -o passwd_file=/home/ubuntu/.passwd-s3fs -o url=http://s3.cn-north-1.amazonaws.com.cn -o endpoint=cn-north-1
sudo sed -i 's/DAEMON_USER=kurento/DAEMON_USER=ubuntu/g' /etc/default/kurento-media-server
sudo service kurento-media-server-6.0 start
