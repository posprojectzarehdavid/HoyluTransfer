using HoyluReceiver;
using Newtonsoft.Json;
using QRCoder;
using Quobject.SocketIoClientDotNet.Client;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.NetworkInformation;
using System.Reflection;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Windows.Media.Imaging;

namespace HoyluReceiverLibrary
{
    public class Receiver
    {

        public Receiver(string ipAddress, string port)
        {
            this.ipAddress = ipAddress;
            this.port = port;
        }

        Socket socket;
        private HoyluDevice hoyluDevice;
        private BitmapImage bitmapImage;
        private string ipAddress;
        private string port;
        private string savePath = System.Environment.GetEnvironmentVariable("USERPROFILE") + @"\Desktop\";

        private bool deviceRegistered = false;
        private bool hasConnected = false;
        private bool hasReceivedFile = false;
        private bool qrUsed = false;

        private System.Drawing.Bitmap qrCodeImage;
        public string SavePath
        {
            get { return savePath; }
            set { savePath = value; }
        }


        private void ConnectToServer()
        {
            Regex getFileExtension = new Regex(@"\w*\.(?<extension>.+)");
            DisconnectSocket();
            socket = IO.Socket($"http://{ipAddress}:{port}");
            socket.On(Socket.EVENT_CONNECT, (fn) =>
            {
                if (hasConnected) return;
                hasConnected = true;
                Console.WriteLine("Connected");
                string hoyluDeviceAsJson = JsonConvert.SerializeObject(hoyluDevice);
                socket.Emit("receiverClient", hoyluDeviceAsJson);
                socket.On("device_registered", () =>
                {
                    if (deviceRegistered) return;
                    deviceRegistered = true;
                    Console.WriteLine("Device registered at server-ip "+ipAddress);
                    if (qrUsed)
                    {
                        qrCodeImage.Save(Path.Combine(Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location), "QRCode.jpg"), System.Drawing.Imaging.ImageFormat.Jpeg);
                        Console.WriteLine("QRCode saved to " + Path.Combine(Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location), "QRCode.jpg"));
                        ShowQR();
                    }                 
                });


                socket.On("getFile", (data) =>
                {
                    if (hasReceivedFile) return;
                    hasReceivedFile = true;
                    Console.WriteLine("File received");
                    System.Diagnostics.Debugger.NotifyOfCrossThreadDependency();
                    ServerFile file = JsonConvert.DeserializeObject<ServerFile>(data.ToString());
                    string url = @"http://"+ipAddress+":"+port+"/file_for_download/:" + file.Filename;
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
                        string pathWithFilename = savePath + file.Originalname;

                        if (match.Groups["extension"].Value == "jpg" || match.Groups["extension"].Value == "bmp" || match.Groups["extension"].Value == "png") //Ist Bild
                        {
                            bitmapImage = ToImage(lnByte);
                        }
                        Console.WriteLine("---------------Saving File---------------------");
                        try
                        {
                            File.WriteAllBytes(pathWithFilename, lnByte);

                        }
                        catch (UnauthorizedAccessException ex)
                        {
                            Console.WriteLine(ex.Message);
                        }
                        if (File.Exists(pathWithFilename)) Console.WriteLine("File "+file.Originalname+" saved successfully");

                        socket.Emit("fileReceived");
                    }



                });
            });
            socket.Connect();
        }

        private void DisconnectSocket()
        {
            Console.WriteLine("----------------> DisconnectSocket");
            if (socket != null)
            {
                socket.Disconnect();
                socket.Off();
                socket = null;
            }
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
        public void Register(string name, bool useBluetooth, bool useQR, bool useNetwork, bool useNFC)
        {
            //string hoyluId = "f67317b7-5823-474b-b8e2-aa36e5564942"; ////Für NFC testen

            string hoyluId = Guid.NewGuid().ToString();
            string bluetoothAddress = "";
            string qrValue = "";
            string nfcValue = "";
            string publicIp = "";
            string defaultGateway = "";

            if (useBluetooth) bluetoothAddress = GetBTMacAddress();
            if (useQR)
            {
                qrUsed = true;
                qrValue = hoyluId;
                QRCodeGenerator qrGenerator = new QRCodeGenerator();
                QRCodeData qrCodeData = qrGenerator.CreateQrCode(qrValue, QRCodeGenerator.ECCLevel.L);
                QRCode qrCode = new QRCode(qrCodeData);
                qrCodeImage = qrCode.GetGraphic(20);
                
            }
            if(useNFC)nfcValue = hoyluId;
            if (useNetwork)
            {
                publicIp = new WebClient().DownloadString(@"http://icanhazip.com").Trim();
                defaultGateway = GetDefaultGatewayAddress();
            }

            hoyluDevice = new HoyluDevice(name, hoyluId, bluetoothAddress, qrValue, nfcValue, publicIp, defaultGateway);
            ConnectToServer();
        }

        private void ShowQR()
        {
            Process.Start("QRCode.jpg");
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
