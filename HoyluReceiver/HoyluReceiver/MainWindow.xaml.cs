﻿using Newtonsoft.Json;
using Quobject.SocketIoClientDotNet.Client;
using System;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net;
using System.Net.NetworkInformation;
using System.Runtime.Serialization.Formatters.Binary;
using System.Security.Cryptography;
using System.Text;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace HoyluReceiver
{
    /// <summary>
    /// Interaktionslogik für MainWindow.xaml
    /// </summary>
    /// 
    public partial class MainWindow : Window
    {
        Socket s;
        string name, hoyluId, bluetoothAddress, qrValue, nfcValue, publicIp, defaultGateway;
        HoyluDevice hoyluDevice;
        BitmapImage bitmapImage;
        string imageString;

        public MainWindow()
        {
            InitializeComponent();
            /*
            bluetoothAddress = GetBTMacAddress();
            hoyluId = Guid.NewGuid().ToString();
            name = Environment.MachineName;
            qrValue = hoyluId;
            nfcValue = hoyluId;
            publicIp = new WebClient().DownloadString(@"http://icanhazip.com").Trim();
            defaultGateway = GetDefaultGatewayAddress();
            hoyluDevice = new HoyluDevice(name, hoyluId, bluetoothAddress, qrValue, nfcValue, publicIp, defaultGateway);
            ConnectToServer();
            */
        }

        private void ConnectToServer()
        {
            s = IO.Socket("http://40.114.246.211:4200");
            s.On(Socket.EVENT_CONNECT, (fn) =>
            {
                Console.WriteLine("Connected");
                s.Emit("client", "WindowsClient");
                string hoyluDeviceAsJson = JsonConvert.SerializeObject(hoyluDevice);
                s.Emit("device_properties", hoyluDeviceAsJson);

                s.On("receiveImage", (data) =>
                {
                    Dispatcher.BeginInvoke(
                       new Action(() =>
                       {
                           imageString += data.ToString();
                       })
                    );
                });

                s.On("receiveChecksum", (data) =>
                {
                    //Console.WriteLine(data.ToString());
                    //Console.WriteLine(imageString);
                    byte[] encoded = new UTF8Encoding().GetBytes(imageString);
                    byte[] hash = ((HashAlgorithm)CryptoConfig.CreateFromName("MD5")).ComputeHash(encoded);
                    if (data.ToString().Equals(hash.ToString()))
                    {
                        Console.WriteLine("Daten wrden vollständig übertragen");
                        Dispatcher.BeginInvoke(
                       new Action(() =>
                       {
                           BeginInit();
                           byte[] x = Convert.FromBase64String(imageString);
                           bitmapImage = ToImage(x);
                           image.Source = bitmapImage;
                           EndInit();
                       })
                    );

                    }
                });
            });
            s.Connect();
        }

        public static byte[] ObjectToByteArray(object obj)
        {
            BinaryFormatter bf = new BinaryFormatter();
            using (var ms = new MemoryStream())
            {
                bf.Serialize(ms, obj);
                return ms.ToArray();
            }
        }

        public BitmapImage ToImage(byte[] byteVal)
        {
            if (byteVal == null)
            {
                return null;
            }

            try
            {
                MemoryStream strmImg = new MemoryStream(byteVal);
                BitmapImage myBitmapImage = new BitmapImage();
                myBitmapImage.BeginInit();
                myBitmapImage.StreamSource = strmImg;
                myBitmapImage.DecodePixelWidth = 200;
                myBitmapImage.EndInit();
                return myBitmapImage;
            }
            catch (Exception)
            {
                return null;
            }
        }

        public static string GetBTMacAddress()
        {
            foreach (NetworkInterface nic in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (nic.NetworkInterfaceType != NetworkInterfaceType.FastEthernetFx &&
                    nic.NetworkInterfaceType != NetworkInterfaceType.Wireless80211 &&
                    (nic.Name.Contains("Bluetooth") || nic.Name.Contains("bluetooth")))
                {
                    return nic.GetPhysicalAddress().ToString();
                }
            }
            return null;
        }

        private void register_Click(object sender, RoutedEventArgs e)
        {
            hoyluId = Guid.NewGuid().ToString();
            if (registerBluetooth.IsChecked == true) bluetoothAddress = GetBTMacAddress();
            if (registerQRCode.IsChecked == true) qrValue = hoyluId;
            if (registerNFC.IsChecked == true) nfcValue = hoyluId;
            if (registerNetwork.IsChecked == true)
            {
                publicIp = new WebClient().DownloadString(@"http://icanhazip.com").Trim();
                defaultGateway = GetDefaultGatewayAddress();
            }
            name = deviceName.Text;
            hoyluDevice = new HoyluDevice(name, hoyluId, bluetoothAddress, qrValue, nfcValue, publicIp, defaultGateway);
            ConnectToServer();
        }

        public static string GetDefaultGatewayAddress()
        {
            NetworkInterface[] adapters = NetworkInterface.GetAllNetworkInterfaces();
            foreach (NetworkInterface adapter in adapters)
            {
                IPInterfaceProperties adapterProperties = adapter.GetIPProperties();
                GatewayIPAddressInformationCollection addresses = adapterProperties.GatewayAddresses;
                if (addresses.Count > 0)
                {
                    Console.WriteLine(adapter.Description);
                    foreach (GatewayIPAddressInformation address in addresses)
                    {
                        if (address.Address.AddressFamily == System.Net.Sockets.AddressFamily.InterNetwork)
                        {
                            return address.Address.ToString();
                        }
                    }
                }
            }
            return null;
        }
    }
}