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
            Receiver seas = new Receiver("40.114.246.211", "4200");
            seas.SavePath = @"C:\Users\David\Desktop\";
            seas.Register("TestUser");
            Console.ReadKey();
        }
    }
}
