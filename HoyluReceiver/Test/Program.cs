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
            Main seas = new Main();
            seas.IpAddress = "40.114.246.211:4200";
            seas.Port = "4200";
            seas.Register("TestUser");
            Console.ReadKey();
        }
    }
}
