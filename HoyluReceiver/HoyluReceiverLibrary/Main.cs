using HoyluReceiver;
using Newtonsoft.Json;
using QRCoder;
using Quobject.SocketIoClientDotNet.Client;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.NetworkInformation;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Windows.Media.Imaging;

namespace HoyluReceiverLibrary
{
    public class Main
    {
        Socket socket;
        private bool deviceRegistered = false;
        private HoyluDevice hoyluDevice;
        private BitmapImage bitmapImage;

        private void ConnectToServer()
        {
            Regex getFileExtension = new Regex(@"\w*\.(?<extension>.+)");
            if (socket != null)
            {
                socket.Disconnect();
                socket.Off();
            }
            socket = IO.Socket("http://40.114.246.211:4200");
            socket.On(Socket.EVENT_CONNECT, (fn) =>
            {
                Console.WriteLine("Connected");
                string hoyluDeviceAsJson = JsonConvert.SerializeObject(hoyluDevice);
                socket.Emit("receiverClient", hoyluDeviceAsJson);
                socket.On("device_registered", () =>
                {
                    deviceRegistered = true;
                });


                socket.On("getFile", (data) =>
                {
                    Console.WriteLine("File received");
                    System.Diagnostics.Debugger.NotifyOfCrossThreadDependency();
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

                    Match match = getFileExtension.Match(file.Originalname);
                    if (match.Success)
                    {
                        string savePath = System.Environment.GetEnvironmentVariable("USERPROFILE") + @"\Desktop\" + file.Originalname;

                        if (match.Groups["extension"].Value == "jpg" || match.Groups["extension"].Value == "bmp" || match.Groups["extension"].Value == "png") //Ist Bild
                        {
                            bitmapImage = ToImage(lnByte);
                        }
                        Console.WriteLine("Saving File");
                        File.WriteAllBytes(savePath, lnByte);


                        socket.Emit("fileReceived");
                    }



                });
            });
            socket.Connect();
        }
        private BitmapImage ToImage(byte[] byteVal)
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

        private static string GetBTMacAddress()
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
        public void Register(string name)
        {
            string hoyluId = Guid.NewGuid().ToString();
            string bluetoothAddress = GetBTMacAddress();

            string qrValue = hoyluId;
            QRCodeGenerator qrGenerator = new QRCodeGenerator();
            QRCodeData qrCodeData = qrGenerator.CreateQrCode(qrValue, QRCodeGenerator.ECCLevel.L);
            QRCode qrCode = new QRCode(qrCodeData);
            System.Drawing.Bitmap qrCodeImage = qrCode.GetGraphic(20);
            qrCodeImage.Save(System.Environment.GetEnvironmentVariable("USERPROFILE") + @"\Desktop\Seas.jpg", System.Drawing.Imaging.ImageFormat.Jpeg);


            string nfcValue = hoyluId;

            string publicIp = new WebClient().DownloadString(@"http://icanhazip.com").Trim();
            string defaultGateway = GetDefaultGatewayAddress();


            hoyluDevice = new HoyluDevice(name, hoyluId, bluetoothAddress, qrValue, nfcValue, publicIp, defaultGateway);
            ConnectToServer();
        }
        private static string GetDefaultGatewayAddress()
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
