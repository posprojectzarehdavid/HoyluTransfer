using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HoyluReceiver
{
    public class ImagePart
    {
        public ImagePart(string part, bool last)
        {
            i = part;
            l = last;
        }        
        public string i { get; set; }
        public bool l { get; set; } 
    }
}
