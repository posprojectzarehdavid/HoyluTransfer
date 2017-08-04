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
            Part = part;
            Last = last;
        }        
        public string Part { get; set; }
        public bool Last { get; set; } 
    }
}
