using Newtonsoft.Json;
using QRCoder;
using Quobject.SocketIoClientDotNet.Client;
using System;
using System.Drawing;
using System.IO;
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
        private bool deviceRegistered = false;

        public MainWindow()
        {
            InitializeComponent();
        }

        private void ConnectToServer()
        {
            Regex getFileExtension = new Regex(@"\w*\.(?<extension>.)+");
            int threadCounter = 1;
            if (socket != null)
            {
                socket.Disconnect();
                socket.Off();
            }
            ProgressDialog connectionDialog = new ProgressDialog();
            connectionDialog.Show();
            socket = IO.Socket("http://40.114.246.211:4200");
            socket.On(Socket.EVENT_CONNECT, (fn) =>
            {
                Console.WriteLine("Connected");
                string hoyluDeviceAsJson = JsonConvert.SerializeObject(hoyluDevice);
                socket.Emit("receiverClient", hoyluDeviceAsJson);
                socket.On("device_registered", () =>
                {
                    Dispatcher.BeginInvoke(
                       new Action(() =>
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
                });


                socket.On("getFile", (data) =>
                {
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

                    Match match = getFileExtension.Match(file.Filename);
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
                                       SaveToDirectory(bitmapImage, savePath, socket);

                                   }
                               }
                               else
                               {
                                   MessageBox.Show("The received filetype couldn't be shown, you can find the file at: " + savePath);
                                   if (qrUsed) qrCodeView.Source = new BitmapImage(new Uri(@"\Resources\error-icon.png"));
                                   else
                                   {
                                       image.Source = new BitmapImage(new Uri(@"\Resources\error-icon.png"));
                                       Canvas.SetTop(image, 50);
                                       Canvas.SetLeft(image, 280);
                                   }
                               }
                           })
                         );
                        socket.Emit("fileReceived");
                    }



                });
            });
            socket.Connect();
        }

        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            if (socket != null)
            {
                socket.Disconnect();
                socket.Off();
            }
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
            config = new Config();
            InitConfiguration();
        }

        private void InitConfiguration()
        {
            configDirectory = System.Environment.GetEnvironmentVariable("USERPROFILE") + @"\Documents\config.csv"; //Standort von config File
            if (File.Exists(configDirectory))
            {
                string[] lines = File.ReadAllLines(configDirectory);
                foreach (string line in lines)
                {
                    config.SaveFileToPath = line.Split(';')[1];
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
            var csv = new StringBuilder();
            var receivedFileDirectory = string.Format("{0};{1}", "saveFilePath", "C:\\USERS\\DAVID\\DESKTOP"); //Pfad wo Bild gespeichert wird
            csv.AppendLine(receivedFileDirectory);
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
                    string path = fbd.SelectedPath;
                    string text = File.ReadAllText(configDirectory);
                    text = text.Replace(config.SaveFileToPath, path);
                    File.WriteAllText(configDirectory, text);
                }
            }

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
            hoyluId = "f67317b7-5823-474b-b8e2-aa36e5564942";

            //hoyluId = Guid.NewGuid().ToString();
            if (registerBluetooth.IsChecked == true) bluetoothAddress = GetBTMacAddress();

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
                defaultGateway = GetDefaultGatewayAddress();
                networkUsed = true;
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
