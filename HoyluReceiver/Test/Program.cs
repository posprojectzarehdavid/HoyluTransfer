using HoyluReceiverLibrary;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Test
{
    class Program
    {
        static void Main(string[] args)
        {
            Receiver testreceiver = new Receiver("40.114.246.211", "4200");
            testreceiver.SavePath = @"C:\Users\David\Desktop\";
            testreceiver.Register("TestUser", false, true, true, true);
            Console.ReadKey();
        }
    }
}
