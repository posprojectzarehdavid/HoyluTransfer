using Newtonsoft.Json;
using Quobject.SocketIoClientDotNet.Client;
using System;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net;
using System.Net.NetworkInformation;
using System.Runtime.Serialization.Formatters.Binary;
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
        Action<BitmapImage> mydelegate;
        BitmapImage bitmapImage;

        public MainWindow()
        {
            InitializeComponent();
            mydelegate = new Action<BitmapImage>(delegate (BitmapImage param)
            {
                image.Source = param;
            });
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
                    //mydelegate.Invoke(bitmapImage);
                    Dispatcher.BeginInvoke(
                       new Action(() =>
                       {
                           //string d = JsonConvert.SerializeObject(data);
                           //string s = Convert.ToBase64String(ObjectToByteArray(data));
                           string decompressed = Decompress(data.ToString());
                           byte[] x = Convert.FromBase64String(decompressed);
                           bitmapImage = ToImage(x);
                           image.Source = bitmapImage;
                       })
                    );
                });
            });
            s.Connect();
        }

        public static void CopyTo(Stream src, Stream dest)
        {
            byte[] bytes = new byte[4096];

            int cnt;

            while ((cnt = src.Read(bytes, 0, bytes.Length)) != 0)
            {
                dest.Write(bytes, 0, cnt);
            }
        }

        public static string Decompress(string compressed)
        {
            byte[] bytes = Convert.FromBase64String(compressed);
            using (var msi = new MemoryStream(bytes))
            using (var mso = new MemoryStream())
            {
                using (var gs = new GZipStream(msi, CompressionMode.Decompress))
                {
                    //gs.CopyTo(mso);
                    CopyTo(gs, mso);
                }

                return Encoding.UTF8.GetString(mso.ToArray());
            }
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
