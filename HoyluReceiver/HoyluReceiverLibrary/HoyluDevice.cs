namespace HoyluReceiver
{
    public class HoyluDevice
    {
        public HoyluDevice(string name, string hoyluId, string bluetoothAddress, string qrValue, string nfcValue, string publicIp, string defaultGateway)
        {
            Name = name;
            HoyluId = hoyluId;
            BluetoothAddress = bluetoothAddress;
            QrValue = qrValue;
            NfcValue = nfcValue;
            PublicIp = publicIp;
            DefaultGateway = defaultGateway;
        }

        public string Name { get; set; }
        public string HoyluId { get; set; }
        public string BluetoothAddress { get; set; }
        public string QrValue { get; set; }
        public string NfcValue { get; set; }
        public string PublicIp { get; set; }
        public string DefaultGateway { get; set; }
    }
}
