using Quobject.SocketIoClientDotNet.Client;
using System;
using System.Net.NetworkInformation;
using System.Windows;

namespace HoyluReceiver
{
    /// <summary>
    /// Interaktionslogik für MainWindow.xaml
    /// </summary>
    /// 
    public partial class MainWindow : Window
    {
        Socket s;
        string bluetoothAddress;

        public MainWindow()
        {
            InitializeComponent();
            ConnectToServer();
            bluetoothAddress = GetBTMacAddress();

        }

        private void ConnectToServer()
        {
            s = IO.Socket("http://40.114.246.211:4200");
            s.On(Socket.EVENT_CONNECT, (fn) =>
            {
                Console.WriteLine("Connected");
                s.Emit("client", "WindowsClient");
            });
            s.Connect();
        }

        public static string GetBTMacAddress()
        {
            foreach (NetworkInterface nic in NetworkInterface.GetAllNetworkInterfaces())
            {
                // Only consider Bluetooth network interfaces
                if (nic.NetworkInterfaceType != NetworkInterfaceType.FastEthernetFx &&
                    nic.NetworkInterfaceType != NetworkInterfaceType.Wireless80211 &&
                    (nic.Name.Contains("Bluetooth") || nic.Name.Contains("bluetooth")))
                {
                    return nic.GetPhysicalAddress().ToString();
                }
            }
            return null;
        }
    }
}
