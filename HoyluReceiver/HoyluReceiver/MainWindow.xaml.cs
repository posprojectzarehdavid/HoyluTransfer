using Newtonsoft.Json;
using QRCoder;
using Quobject.SocketIoClientDotNet.Client;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.NetworkInformation;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;




namespace HoyluReceiver
{
    public partial class MainWindow : Window
    {
        Socket socket;
        string name, hoyluId, bluetoothAddress, qrValue, nfcValue, publicIp, defaultGateway;
        private bool networkUsed = false;
        HoyluDevice hoyluDevice;
        BitmapImage bitmapImage;
        private System.Windows.Point mousePosition;
        private System.Windows.Controls.Image draggedImage;
        bool qrUsed = false;
        string storyboard;
        Config config;
        private string configDirectory;
        private Bitmap qrCodeImage;
        private bool nfcUsed = false;
        private List<HoyluDevice> hoyluDevices;
        private StringBuilder csv = new StringBuilder();
        CultureInfo provider = CultureInfo.InvariantCulture;
        private static string datePattern = "dd.MM.yyyy HH:mm:ss";

        private bool deviceRegistered = false;
        private bool hasConnected = false;
        private bool hasReceivedFile = false;

        public MainWindow()
        {
            InitializeComponent();
        }

        private void ConnectToServer()
        {
            Console.WriteLine("****************** ConnectToServer ***************");
            Regex getFileExtension = new Regex(@"\w*\.(?<extension>.+)");
            int threadCounter = 1;
            DisconnectSocket();
            ProgressDialog connectionDialog = new ProgressDialog();
            connectionDialog.Show();
            socket = IO.Socket("http://40.114.246.211:4200");
            socket.On(Socket.EVENT_CONNECT, (fn) =>
            {
                if (hasConnected) return;
                hasConnected = true;
                Console.WriteLine("----------------> Connected");
                string hoyluDeviceAsJson = JsonConvert.SerializeObject(hoyluDevice);
                socket.Emit("receiverClient", hoyluDeviceAsJson);
                socket.On("device_registered", () =>
                {
                    if (deviceRegistered) return;
                    deviceRegistered = true;
                    Console.WriteLine("----------------> device_registered");
                    threadCounter = OnDeviceRegisterd(threadCounter, connectionDialog);
                });


                socket.On("getFile", (data) =>
                {
                    if (hasReceivedFile) return;
                    hasReceivedFile = true;
                    Console.WriteLine("----------------> getFile");
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
                        string savePath = config.SaveFileToPath + "\\" + file.Originalname;

                        Dispatcher.BeginInvoke(
                           new Action(() =>
                           {

                               if (match.Groups["extension"].Value == "jpg" || match.Groups["extension"].Value == "bmp" || match.Groups["extension"].Value == "png") //Ist Bild
                               {
                                   bitmapImage = ToImage(lnByte);
                                   if (bitmapImage != null)
                                   {
                                       if (qrUsed)
                                       {
                                           qrCodeView.Source = bitmapImage;
                                       }
                                       else
                                       {
                                           image.Source = bitmapImage;
                                           Canvas.SetTop(image, 50);
                                           Canvas.SetLeft(image, 280);
                                       }

                                   }
                               }
                               else
                               {
                                   MessageBox.Show("The received filetype couldn't be shown, you can find the file at: " + savePath);
                                   if (qrUsed) qrCodeView.Source = new BitmapImage(new Uri(@"/HoyluReceiver; Resources\error-icon.png", UriKind.Relative));
                                   else
                                   {
                                       image.Source = new BitmapImage(new Uri(@"/HoyluReceiver; Resources\error-icon.png", UriKind.Relative));
                                       Canvas.SetTop(image, 50);
                                       Canvas.SetLeft(image, 280);
                                   }
                                   File.WriteAllBytes(savePath, lnByte);
                               }
                           })
                         );
                        socket.Emit("fileReceived");
                    }



                });
            });
            socket.Connect();
        }

        private int OnDeviceRegisterd(int threadCounter, ProgressDialog connectionDialog)
        {
            Dispatcher.BeginInvoke(new Action(() =>
                                  {
                                      connectionDialog.Close();

                                      deviceRegistered = true;

                                      if (qrUsed && deviceRegistered && threadCounter == 1)
                                      {
                                          qrCodeView.Source = BitmapToImageSource(qrCodeImage);
                                          Canvas.SetTop(qrCodeView, 50);
                                          Canvas.SetLeft(qrCodeView, 280);
                                      }
                                      if (nfcUsed && deviceRegistered && threadCounter == 1)
                                      {
                                          Console.WriteLine("NFC Value:" + nfcValue);
                                          MessageBoxEditText msg = new MessageBoxEditText();
                                          msg.HoyluId.Text = nfcValue;
                                          msg.ShowDialog();
                                      }
                                      if (networkUsed && deviceRegistered && threadCounter == 1) ipaddress.Content = "Public IP: " + publicIp; default_gateway.Content = "Defaultgateway: " + defaultGateway;
                                      threadCounter++;
                                  })
                                );
            return threadCounter;
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

        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            foreach (var hoyluDevice in hoyluDevices)
            {
                WriteToFile($"{hoyluDevice.Name};{hoyluDevice.HoyluId};{hoyluDevice.BluetoothAddress};{hoyluDevice.QrValue};" +
                    $"{hoyluDevice.NfcValue};{hoyluDevice.PublicIp};{hoyluDevice.DefaultGateway};" +
                    $"{hoyluDevice.TimestampLastUsed}", configDirectory);
            }
            WriteToFile($"saveFilePath;{config.SaveFileToPath}", configDirectory);
            DisconnectSocket();
        }



        private void Grid_MouseRightButtonDown(object sender, MouseButtonEventArgs e)
        {
            var image = e.Source as System.Windows.Controls.Image;

            if (image != null && MainViewGrid.CaptureMouse() && (image.Name == "qrCodeView" || image.Name == "image"))
            {
                Console.WriteLine("Dragging");

                mousePosition = e.GetPosition(this);
                Console.WriteLine("ButtonDown Position: X" + mousePosition.X + "| Y" + mousePosition.Y);

                draggedImage = image;
                Canvas.SetTop(draggedImage, mousePosition.Y);
                Canvas.SetLeft(draggedImage, mousePosition.X);
                Console.WriteLine("Image start: X" + Canvas.GetLeft(draggedImage) + " | Y" + Canvas.GetTop(draggedImage));
            }
        }

        private void MainViewGrid_MouseRightButtonUp(object sender, MouseButtonEventArgs e)
        {
            if (draggedImage != null)
            {

                var position = e.GetPosition(this);
                //Console.WriteLine("ButtonUp Position: X" + mousePosition.X + "| Y" + mousePosition.Y);
                //Panel.SetZIndex(draggedImage, 0);

                if (position.X < 0
                    || position.X > this.Width
                         || position.Y < 0
                              || position.Y > this.Height - 10)
                {
                    Canvas.SetTop(draggedImage, 50);
                    Canvas.SetLeft(draggedImage, 280);
                }
                else
                {
                    Canvas.SetTop(draggedImage, position.Y);
                    Canvas.SetLeft(draggedImage, position.X);
                }


                Console.WriteLine("ButtonU Position Image: X" + Canvas.GetLeft(draggedImage) + "| Y" + Canvas.GetTop(draggedImage));

                //draggedImage.Margin = new Thickness(mousePosition.X, mousePosition.Y, draggedImage.Margin.Right, draggedImage.Margin.Bottom);
                MainViewGrid.ReleaseMouseCapture();
                draggedImage = null;

            }
        }

        private void MainViewGrid_MouseMove(object sender, MouseEventArgs e)
        {
            if (draggedImage != null)
            {

                var position = e.GetPosition(this);
                //var xVal = position.X - mousePosition.X;
                //var yVal = position.Y - mousePosition.Y;
                //offset.X = xVal;
                //offset.Y = yVal;

                //var currX = mousePosition.X + offset.X;
                //var currY = mousePosition.Y + offset.Y;
                //Console.WriteLine("ButtonMove Position: X" + currX + "| Y" + currY);
                Canvas.SetTop(draggedImage, position.Y);
                Canvas.SetLeft(draggedImage, position.X);

                //draggedImage.Margin = new Thickness(currX, currY, 0, 0);
                //Console.WriteLine("ButtonMove Position Image: X" + draggedImage.Margin.Left+ "| Y" + draggedImage.Margin.Top);

            }
        }

        private void btnLeftMenuHide_Click(object sender, RoutedEventArgs e)
        {
            ShowHideMenu("sbHideLeftMenu", btnLeftMenuHide, btnLeftMenuShow, pnlLeftMenu);
        }

        private void btnLeftMenuShow_Click(object sender, RoutedEventArgs e)
        {
            ShowHideMenu("sbShowLeftMenu", btnLeftMenuHide, btnLeftMenuShow, pnlLeftMenu);
        }

        private void ShowHideMenu(string storyboard, Button btnLeftMenuHide, Button btnLeftMenuShow, StackPanel pnlLeftMenu)
        {
            Storyboard sb = Resources[storyboard] as Storyboard;
            this.storyboard = storyboard;
            if (storyboard.Contains("Show"))
            {
                ColumnRegister.Width = new GridLength(15, GridUnitType.Star);
                btnLeftMenuHide.Visibility = System.Windows.Visibility.Visible;
                btnLeftMenuShow.Visibility = System.Windows.Visibility.Hidden;
            }
            else if (storyboard.Contains("Hide"))
            {
                sb.Completed += Sb_Completed;
                btnLeftMenuHide.Visibility = System.Windows.Visibility.Hidden;
                btnLeftMenuShow.Visibility = System.Windows.Visibility.Visible;
            }
            sb.Begin(pnlLeftMenu);

        }

        private void Sb_Completed(object sender, EventArgs e)
        {
            ColumnRegister.Width = new GridLength(2.5, GridUnitType.Star);
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            hoyluDevices = new List<HoyluDevice>();
            config = new Config();
            InitConfiguration();
        }

        private void InitConfiguration()
        {
            configDirectory = System.Environment.GetEnvironmentVariable("USERPROFILE") + @"\Documents\config.csv";
            if (File.Exists(configDirectory))
            {
                string[] lines = File.ReadAllLines(configDirectory);
                foreach (string line in lines)
                {
                    string[] splitted = line.Split(';');
                    if (splitted.Length == 2)
                    {
                        config.SaveFileToPath = splitted[1];
                        path.Content = config.SaveFileToPath;
                    }
                    else if (splitted.Length != 0)
                    {
                        HoyluDevice device = new HoyluDevice(splitted[0], splitted[1], splitted[2], splitted[3], splitted[4], splitted[5], splitted[6]);
                        device.TimestampLastUsed = DateTime.ParseExact(splitted[7], datePattern, provider);
                        var dateDifference = (DateTime.Today - device.TimestampLastUsed).TotalDays;
                        if (dateDifference <= 7)
                        {
                            hoyluDevices.Add(device);
                            registeredDevices.Items.Add(device);
                        }

                    }
                }

            }
            else
            {
                CreateInitFile(configDirectory);
                InitConfiguration(); //Erneuter Aufruf sodass Config Objekt befüllt wird
            }
        }

        private void CreateInitFile(string configDirectory)
        {
            var receivedFileDirectory = string.Format("{0};{1}", "saveFilePath", "C:\\USERS\\DAVID\\DESKTOP"); //Pfad wo Bild gespeichert wird
            WriteToFile(receivedFileDirectory, configDirectory);
        }

        private void WriteToFile(string text, string configDirectory)
        {
            csv.AppendLine(text);
            Console.WriteLine(configDirectory, csv.ToString());
            File.WriteAllText(configDirectory, csv.ToString());
        }

        private void changeDirectory_Click(object sender, RoutedEventArgs e)
        {
            using (var fbd = new System.Windows.Forms.FolderBrowserDialog())
            {
                System.Windows.Forms.DialogResult result = fbd.ShowDialog();

                if (result == System.Windows.Forms.DialogResult.OK && !string.IsNullOrWhiteSpace(fbd.SelectedPath))
                {
                    string selPath = fbd.SelectedPath;
                    string text = File.ReadAllText(configDirectory);
                    config.SaveFileToPath = selPath;
                    text = text.Replace(config.SaveFileToPath, selPath);
                    File.WriteAllText(configDirectory, text);
                    path.Content = config.SaveFileToPath;
                }
            }

        }

        private void registeredDevices_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            HoyluDevice device = (HoyluDevice)registeredDevices.SelectedItem;
            hoyluDevices.Remove(device);
            device.TimestampLastUsed = DateTime.Now;
            hoyluDevices.Add(device);

            deviceName.Text = device.Name;

            registerBluetooth.IsChecked = (device.BluetoothAddress != null && device.BluetoothAddress != "") ? true : false;
            registerNFC.IsChecked = (device.NfcValue != null && device.NfcValue != "") ? true : false;
            registerQRCode.IsChecked = (device.QrValue != null && device.QrValue != "") ? true : false;
            registerNetwork.IsChecked = (device.PublicIp != null && device.PublicIp != "") ? true : false;
        }

        public static void SaveToDirectory(BitmapImage image, string filePath, Socket s)
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
            //hoyluId = "f67317b7-5823-474b-b8e2-aa36e5564942"; ////NFC Hoylu Test-ID

            hoyluId = Guid.NewGuid().ToString();
            if (registerBluetooth.IsChecked == true)
            {
                bluetoothAddress = GetBTMacAddress();
                if (bluetoothAddress == null) MessageBox.Show("Bluetooth not available or supported on this device!");
            }

            if (registerQRCode.IsChecked == true)
            {
                qrValue = hoyluId;
                QRCodeGenerator qrGenerator = new QRCodeGenerator();
                QRCodeData qrCodeData = qrGenerator.CreateQrCode(qrValue, QRCodeGenerator.ECCLevel.L);
                QRCode qrCode = new QRCode(qrCodeData);
                qrCodeImage = qrCode.GetGraphic(20);
                qrUsed = true;
            }


            if (registerNFC.IsChecked == true)
            {
                nfcValue = hoyluId;
                Console.WriteLine("NFC enabled");
                nfcUsed = true;
            }
            if (registerNetwork.IsChecked == true)
            {
                publicIp = new WebClient().DownloadString(@"http://icanhazip.com").Trim();
                defaultGateway = GetDefaultGateway();
                networkUsed = true;
            }

            name = deviceName.Text;
            hoyluDevice = new HoyluDevice(name, hoyluId, bluetoothAddress, qrValue, nfcValue, publicIp, defaultGateway);
            hoyluDevice.TimestampLastUsed = DateTime.Now;
            hoyluDevices.Add(hoyluDevice);
            registeredDevices.Items.Add(hoyluDevice);
            ConnectToServer();
            Button register = sender as Button;
            register.IsEnabled = false;
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

        public static string GetDefaultGateway()
        {
            var gateway_address = NetworkInterface.GetAllNetworkInterfaces()
                .Where(e => e.OperationalStatus == OperationalStatus.Up)
                .SelectMany(e => e.GetIPProperties().GatewayAddresses)
                .FirstOrDefault();
            if (gateway_address == null) return null;
            Console.WriteLine(gateway_address.Address);
            return gateway_address.Address.ToString();
        }
    }
}
