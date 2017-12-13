using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HoyluReceiver
{
    public class ServerFile
    {
        public ServerFile(string filename, string originalname)
        {
            Filename = filename;
            Originalname = originalname;
        }        
        public string Filename { get; set; }
        public string Originalname { get; set; } 
    }
}
