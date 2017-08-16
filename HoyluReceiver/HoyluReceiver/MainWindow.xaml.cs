using Newtonsoft.Json;
using QRCoder;
using Quobject.SocketIoClientDotNet.Client;
using System;
using System.Drawing;
using System.IO;
using System.Net;
using System.Net.NetworkInformation;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media.Imaging;

namespace HoyluReceiver
{
    public partial class MainWindow : Window
    {
        Socket s;
        string name, hoyluId, bluetoothAddress, qrValue, nfcValue, publicIp, defaultGateway;
        HoyluDevice hoyluDevice;
        BitmapImage bitmapImage;

        public MainWindow()
        {
            InitializeComponent();
        }

        private void ConnectToServer()
        {
            if(s != null)
            {
                s.Disconnect();
                s.Off();
            }
            ProgressDialog dialog = new ProgressDialog();
            dialog.Show();
            s = IO.Socket("http://40.114.246.211:4200");
            s.On(Socket.EVENT_CONNECT, (fn) =>
            {
                Console.WriteLine("Connected");
                string hoyluDeviceAsJson = JsonConvert.SerializeObject(hoyluDevice);
                s.Emit("windowsClient", hoyluDeviceAsJson);
                s.On("device_registered", () =>
                {
                    Dispatcher.BeginInvoke(
                       new Action(() =>
                       {
                           dialog.Close();
                       })
                    );
                });

                s.On("getImage", (data) =>
                {
                    ServerFile file = JsonConvert.DeserializeObject<ServerFile>(data.ToString());
                    string url = @"http://40.114.246.211:4200/file_for_download/:" + file.Filename;
                    byte[] lnByte;

                    HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
                    string lsResponse = string.Empty;

                    using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
                    {
                        using (BinaryReader reader = new BinaryReader(response.GetResponseStream()))
                        {
                            lnByte = reader.ReadBytes(1 * 1024 * 1024 * 10);
                            using (FileStream stream = new FileStream(file.Filename, FileMode.Create))
                            {
                                stream.Write(lnByte, 0, lnByte.Length);
                            }
                        }
                        response.Close();
                    }

                    string desktoppath = Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory) + "\\" + file.Originalname;

                    Dispatcher.BeginInvoke(
                       new Action(() =>
                       {
                           bitmapImage = ToImage(lnByte);
                           if (bitmapImage != null)
                           {
                               image.Source = bitmapImage;
                               SaveOnDesktop(bitmapImage, desktoppath, s);
                               s.Emit("imageReceived");
                               Console.WriteLine("Hallo");
                           }
                       })
                    );
                });
            });
            s.Connect();
        }

        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            if(s != null)
            {
                s.Disconnect();
                s.Off();
            }
        }

        public static void SaveOnDesktop(BitmapImage image, string filePath, Socket s)
        {
            BitmapEncoder encoder = new PngBitmapEncoder();
            encoder.Frames.Add(BitmapFrame.Create(image));

            using (var fileStream = new FileStream(filePath, FileMode.Create))
            {
                encoder.Save(fileStream);
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
                myBitmapImage.CacheOption = BitmapCacheOption.OnLoad;
                myBitmapImage.EndInit();
                myBitmapImage.Freeze();
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

            qrValue = hoyluId;
            QRCodeGenerator qrGenerator = new QRCodeGenerator();
            QRCodeData qrCodeData = qrGenerator.CreateQrCode(qrValue, QRCodeGenerator.ECCLevel.L);
            QRCode qrCode = new QRCode(qrCodeData);
            Bitmap qrCodeImage = qrCode.GetGraphic(20);
            qrCodeView.Source = BitmapToImageSource(qrCodeImage);

            if (registerNFC.IsChecked == true) nfcValue = hoyluId;
            if (registerNetwork.IsChecked == true)
            {
                publicIp = new WebClient().DownloadString(@"http://icanhazip.com").Trim();
                defaultGateway = GetDefaultGatewayAddress();
            }

            name = deviceName.Text;
            hoyluDevice = new HoyluDevice(name, hoyluId, bluetoothAddress, qrValue, nfcValue, publicIp, defaultGateway);
            ConnectToServer();
            Button x = sender as Button;
            x.IsEnabled = false;
        }

        BitmapImage BitmapToImageSource(Bitmap bitmap)
        {
            using (MemoryStream memory = new MemoryStream())
            {
                bitmap.Save(memory, System.Drawing.Imaging.ImageFormat.Bmp);
                memory.Position = 0;
                BitmapImage bitmapimage = new BitmapImage();
                bitmapimage.BeginInit();
                bitmapimage.StreamSource = memory;
                bitmapimage.CacheOption = BitmapCacheOption.OnLoad;
                bitmapimage.EndInit();
                return bitmapimage;
            }
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
