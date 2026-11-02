using UnityEngine;

namespace PviHarness.Replay
{
    /// <summary>
    /// The AprilTag 36h11 family (the tags FTC fields use): the 587 codes and how each code's 36
    /// bits sit in the tag's square, so the replay can draw a field's tags with their real black and
    /// white patterns (a camera, or the webcam picture, sees the same tag as on the real field).
    ///
    /// <para>The codes are copied from the AprilTag library the FTC SDK uses (OpenFTC's
    /// <c>org.openftc:apriltag:2.1.0</c>, AprilTag 3 from the University of Michigan, BSD 2-clause
    /// licence), read from its <c>libapriltag.so</c> on this computer. Checked: tag 0 and tag 1 drawn
    /// this way match the older AprilTag 2 codes laid out row by row.</para>
    ///
    /// <para>A tag is 10 x 10 cells: a white edge, a black border, and 6 x 6 data cells. Bit 35 (the
    /// highest) is the first cell in <see cref="BitCells"/>; a 1 bit is white.</para>
    /// </summary>
    public static class AprilTag36h11
    {
        /// <summary>Cells across a whole tag, including the white edge (the tag's size is the black square: 8 cells).</summary>
        public const int Cells = 10;

        /// <summary>The family's codes, by tag id (0 to 586).</summary>
        public static readonly long[] Codes =
        {
            0xD7E00984BL, 0xDDA664CA7L, 0xDC4A1C821L, 0xE17B470E9L, 0xEF91D01B1L, 0xF429CDD73L,
            0x05DA29225L, 0x1106CBA43L, 0x223BED79DL, 0x21F51213CL, 0x33EB19CA6L, 0x3F76EB0F8L,
            0x469A97414L, 0x45DCFE0B0L, 0x4A6465F72L, 0x51801DB96L, 0x5EB946B4EL, 0x68A7CC2ECL,
            0x6F0BA2652L, 0x78765559DL, 0x87B83D129L, 0x86CC4A5C5L, 0x8B64DF90FL, 0x9C577B611L,
            0xA3810F2F5L, 0xAF4D75B83L, 0xB59A03FEFL, 0xBB1096F85L, 0xD1B92FC76L, 0xD0DD509D2L,
            0xE2CFDA160L, 0x2FF497C63L, 0x47240671BL, 0x5047A2E55L, 0x635CA87C7L, 0x691254166L,
            0x68F43D94AL, 0x6EF24BDB6L, 0x8CDD8F886L, 0x9DE96B718L, 0xAFF6E5A8AL, 0xBAE46F029L,
            0xD225B6D59L, 0xDF8BA8C01L, 0xE3744A22FL, 0xFBB59375DL, 0x18A916828L, 0x22F29C1BAL,
            0x286887D58L, 0x41392322EL, 0x75D18ECD1L, 0x87C302743L, 0x8C6317BA9L, 0x9E40F36D7L,
            0xC0E5A806AL, 0xCC78CB87CL, 0x12D2F2D01L, 0x379F36A21L, 0x6973F59ACL, 0x7789EA9F4L,
            0x8F1C73E84L, 0x8DD287A20L, 0x94A4EEE4CL, 0xA455379B5L, 0xA9E92987DL, 0xBD25CB40BL,
            0xBE98D3582L, 0xD3D5972B2L, 0x14C53D7C7L, 0x4F1796936L, 0x4E71FED1AL, 0x66D46FAE0L,
            0xA55ABB933L, 0xEBEE1ACCAL, 0x1AD4BA6A4L, 0x305B17571L, 0x553611351L, 0x59CA62775L,
            0x7819CB6A1L, 0xEDB7BC9EBL, 0x5B2694212L, 0x72E12D185L, 0xED6152E2CL, 0x5BCDADBF3L,
            0x78E0AA0C6L, 0xC60A0B909L, 0xEF9A34B0DL, 0x398A6621AL, 0xA8A27C944L, 0x4B564304EL,
            0x52902B4E2L, 0x857280B56L, 0xA91B2C84BL, 0xE91DF939BL, 0x1FA405F28L, 0x23793AB86L,
            0x68C17729FL, 0x9FBF3B840L, 0x36922413CL, 0x4EB5F946EL, 0x533FE2404L, 0x63DE7D35EL,
            0x925EDDC72L, 0x99B8B3896L, 0xAACE4C708L, 0xC22994AF0L, 0x8F1EAE41BL, 0xD95FB486CL,
            0x13FB77857L, 0x4FE0983A3L, 0xD559BF8A9L, 0xE1855D78DL, 0xFEC8DAAADL, 0x71ECB6D95L,
            0xDC9E50E4CL, 0xCA3A4C259L, 0x740D12BBFL, 0xAEEDD18E0L, 0xB509B9C8EL, 0x5232FEA1CL,
            0x19282D18BL, 0x76C22D67BL, 0x936BEB34BL, 0x08A5EA8DDL, 0x679EADC28L, 0xA08E119C5L,
            0x20A6E3E24L, 0x7EAB9C239L, 0x96632C32EL, 0x470D06E44L, 0x8A70212FBL, 0x0A7E4251BL,
            0x9EC762CC0L, 0xD8A3A1F48L, 0xDB680F346L, 0x4A1E93A9DL, 0x638DDC04FL, 0x4C2FCC993L,
            0x01EF28C95L, 0xBF0D9792DL, 0x6D27557C3L, 0x623F977F4L, 0x35B43BE57L, 0xBB0C428D5L,
            0xA6F01474DL, 0x5A70C9749L, 0x20DDABC3BL, 0x2EABD78CFL, 0x90AA18F88L, 0xA9EA89350L,
            0x3CDB39B22L, 0x839A08F34L, 0x169BB814EL, 0x1A575AB08L, 0xA04D3D5A2L, 0xBF7902F2BL,
            0x095A5E65CL, 0x92E8FCE94L, 0x67EF48D12L, 0x6400DBCACL, 0xB12D8FB9FL, 0x0347F45D3L,
            0xB35826F56L, 0xC546AC6E4L, 0x81CC35B66L, 0x41D14BD57L, 0x0C052B168L, 0x7D6CE5018L,
            0xAB4ED5EDEL, 0x5AF817119L, 0xD1454B182L, 0x2BADB090BL, 0x03FCB4C0CL, 0x2F1C28FD8L,
            0x93608C6F7L, 0x4C93BA2B5L, 0x07D950A5DL, 0xE54B3D3FCL, 0x15560CF9DL, 0x189E4958AL,
            0x62140E9D2L, 0x723BC1CDBL, 0x2063F26FAL, 0xFA08AB19FL, 0x7955641DBL, 0x646B01DAAL,
            0x71CD427CCL, 0x09A42F7D4L, 0x717EDC643L, 0x15EB94367L, 0x8392E6BB2L, 0x832408542L,
            0x2B9B874BEL, 0xB21F4730DL, 0xB5D8F24C9L, 0x7DBAF6931L, 0x1B4E33629L, 0x13452E710L,
            0xE974AF612L, 0x1DF61D29AL, 0x99F2532ADL, 0xE50EC71B4L, 0x5DF0A36E8L, 0x4934E4CEAL,
            0xE34A0B4BDL, 0xB7B26B588L, 0x0F255118DL, 0xD0C8FA31EL, 0x06A50C94FL, 0xF28AA9F06L,
            0x131D194D8L, 0x622E3DA79L, 0xAC7478303L, 0xC8F2521D7L, 0x6C9C881F5L, 0x49E38B60AL,
            0x513D8DF65L, 0xD7C2B0785L, 0x9F6F9D75AL, 0x9F6966020L, 0x1E1A54E33L, 0xC04D63419L,
            0x946E04CD7L, 0x1BDAC5902L, 0x56469B830L, 0xFFAD59569L, 0x86970E7D8L, 0x8A4B41E12L,
            0xAD4688E3BL, 0x85F8F5DF4L, 0xD833A0893L, 0x2A36FDD7CL, 0xD6A857CF2L, 0x8829BC35CL,
            0x5E50D79BCL, 0xFBB8035E4L, 0xC1A95BEBFL, 0x036B0BAF8L, 0xE0DA964EAL, 0xB6483689BL,
            0x7C8E2F4C1L, 0x5B856A23BL, 0x2FC183995L, 0xE914B6D70L, 0xB31041969L, 0x1BB478493L,
            0x063E2B456L, 0xF2A082B9CL, 0x8E5E646EAL, 0x08172F8F6L, 0x0DACD923EL, 0xE5DCF0E2EL,
            0xBF9446BAEL, 0x4822D50D1L, 0x26E710BF5L, 0xB90BA2A24L, 0xF3B25AA73L, 0x809AD589BL,
            0x94CC1E254L, 0x5334A3ADBL, 0x592886B2FL, 0xBF64704AAL, 0x566DBF24CL, 0x72203E692L,
            0x64E61E809L, 0xD7259AAD6L, 0x7B924AEDCL, 0x2DF2184E8L, 0x353D1ECA7L, 0xFCE30D7CEL,
            0xF7B0F436EL, 0x57E8D8F68L, 0x8C79E60DBL, 0x9C8362B2BL, 0x63A5804F2L, 0x9298353DCL,
            0x6F98A71C8L, 0xA5731F693L, 0x21CA5C870L, 0x1C2107FD3L, 0x6181F6C39L, 0x19E574304L,
            0x329937606L, 0x043D5C70DL, 0x9B18FF162L, 0x8E2CCFEBFL, 0x72B7B9B54L, 0x9B71F4F3CL,
            0x935D7393EL, 0x65938881AL, 0x6A5BD6F2DL, 0xA19783306L, 0xE6472F4D7L, 0x81163DF5AL,
            0xA838E1CBDL, 0x982748477L, 0x050C54FEBL, 0x0D82FBB58L, 0x2C4C72799L, 0x97D259AD6L,
            0x22D9A43EDL, 0xFDB162A9FL, 0x0CB4A727DL, 0x4FAE2E371L, 0x535B5BE8BL, 0x48795908AL,
            0xCE7C18962L, 0x4EA154D80L, 0x50C064889L, 0x8D97FC75DL, 0xC8BD9EC61L, 0x83EE8E8BBL,
            0xC8431419AL, 0x1AA78079DL, 0x8111AA4A5L, 0xDFA3A69FEL, 0x51630D83FL, 0x2D930FB3FL,
            0x2133116E5L, 0xAE5395522L, 0xBC07A4E8AL, 0x57BF08BA0L, 0x6CB18036AL, 0xF0E2E4B75L,
            0x3EB692B6FL, 0xD8178A3FAL, 0x238CCE6A6L, 0xE97D5CDD7L, 0xFE10D8D5EL, 0xB39584A1DL,
            0xCA03536FDL, 0xAA61F3998L, 0x72FF23EC2L, 0x15AA7D770L, 0x57A3A1282L, 0xD1F3902DCL,
            0x6554C9388L, 0xFD01283C7L, 0xE8BAA42C5L, 0x72CEE6ADFL, 0xF6614B3FAL, 0x95C3778A2L,
            0x7DA4CEA7AL, 0xD18A5912CL, 0xD116426E5L, 0x27C17BC1CL, 0xB95B53BC1L, 0xC8F937A05L,
            0xED220C9BDL, 0x0C97D72ABL, 0x8FB1217AEL, 0x25CA8A5A1L, 0xB261B871BL, 0x1BEF0A056L,
            0x806A51179L, 0xEED249145L, 0x3F82AECEBL, 0xCC56E9ACFL, 0x2E78D01EBL, 0x102CEE17FL,
            0x37CAAD3D5L, 0x16AC5B1EEL, 0x2AF164ECEL, 0xD4CD81DC9L, 0x12263A7E7L, 0x57AC7D117L,
            0x9391D9740L, 0x7AEDAA77FL, 0x9675A3C72L, 0x277F25191L, 0xEBB6E64B9L, 0x7AD3EF747L,
            0x12759B181L, 0x948257D4DL, 0xB63A850F6L, 0x3A52A8F75L, 0x4A019532CL, 0xA021A7529L,
            0xCC661876DL, 0x4085AFD05L, 0xE7048E089L, 0x3F979CDC6L, 0xD9DA9071BL, 0xED2FC5B68L,
            0x79D64C3A1L, 0xFD44E2361L, 0x8EEA46A74L, 0x42233B9C2L, 0xAE4D1765DL, 0x7303A094CL,
            0x2D7033ABEL, 0x3DCC2B0B4L, 0x0F0967D09L, 0x06F0CD7DEL, 0x09807ACA0L, 0x3A295CAD3L,
            0x2B106B202L, 0x3F38A828EL, 0x78AF46596L, 0xBDA2DC713L, 0x9A8C8C9D9L, 0x6A0F2DDCEL,
            0xA76AF6FE2L, 0x086F66FA4L, 0xD52D63F8DL, 0x89F7A6E73L, 0xCC6B23362L, 0xB4EBF3C39L,
            0x564F300FAL, 0xE8DE3A706L, 0x79A033B61L, 0x765E160C5L, 0xA266A4F85L, 0xA68C38C24L,
            0xDCA0711FBL, 0x85FBA85BAL, 0x37A207B46L, 0x158FCC4D0L, 0x0569D79B3L, 0x7B1A25555L,
            0xA8AE22468L, 0x7C592BDFDL, 0x0C59A5F66L, 0xB1115DAA3L, 0xF17C87177L, 0x6769D766BL,
            0x2B637356DL, 0x13D8685ACL, 0xF24CB6EC0L, 0x0BD0B56D1L, 0x42FF0E26DL, 0xB41609267L,
            0x96F9518AFL, 0xC56F96636L, 0x4A8E10349L, 0x863512171L, 0xEA455D86CL, 0xBD0E25279L,
            0xE65E3F761L, 0x36C84A922L, 0x85FD1B38FL, 0x657C91539L, 0x15033FE04L, 0x09051C921L,
            0xAB27D80D8L, 0xF92F7D0A1L, 0x8EB6BB737L, 0x10B5B0F63L, 0x6C9C7AD63L, 0xF66FE70AEL,
            0xCA579BD92L, 0x956198E4DL, 0x29E4405E5L, 0xE44EB885CL, 0x41612456CL, 0xEA45E0ABFL,
            0xD326529BDL, 0x7B2C33CEFL, 0x80BC9B558L, 0x7169B9740L, 0xC37F99209L, 0x31FF6DAB9L,
            0xC795190EDL, 0xA7636E95FL, 0x9DF075841L, 0x55A083932L, 0xA7CBDF630L, 0x409EA4EF0L,
            0x92A1991B6L, 0x4B078DEE9L, 0xAE18CE9E4L, 0x5A6E1EF35L, 0x1A403BD59L, 0x31EA70A83L,
            0x2BC3C4F3AL, 0x5C921B3CBL, 0x042DA05C5L, 0x1F667D16BL, 0x416A368CFL, 0xFBC0A7A3BL,
            0x9419F0C7CL, 0x81BE2FA03L, 0x34E2C172FL, 0x28648D8AEL, 0xC7ACBB885L, 0x45F31EB6AL,
            0xD1CFC0A7BL, 0x42C4D260DL, 0xCF6584097L, 0x94B132B14L, 0x3C5C5DF75L, 0x8AE596FEFL,
            0xAEA8054EBL, 0x0AE9CC573L, 0x496FB731BL, 0xEBF105662L, 0xAF9C83A37L, 0xC0D64CD6BL,
            0x7B608159AL, 0xE74431642L, 0xD6FB9D900L, 0x291E99DE0L, 0x10500BA9AL, 0x5CD05D037L,
            0xA87254FB2L, 0x9D7824A37L, 0x8B2C7B47CL, 0x30C788145L, 0x2F4E5A8BEL, 0xBADB884DAL,
            0x026E0D5C9L, 0x6FDBAA32EL, 0x34758EB31L, 0x565CD1B4FL, 0x2BFD90FB0L, 0x093052A6BL,
            0xD3C13C4B9L, 0x2DAEA43BFL, 0xA279762BCL, 0xF1BD9F22CL, 0x4B7FEC94FL, 0x545761D5AL,
            0x7327DF411L, 0x1B52A442EL, 0x49B0CE108L, 0x24C764BC8L, 0x374563045L, 0xA3E8F91C6L,
            0x0E6BD2241L, 0xE0E52EE3CL, 0x07E8E3CAAL, 0x96C2B7372L, 0x33ACBDFDAL, 0xB15D91E54L,
            0x464759AC1L, 0x6886A1998L, 0x57F5D3958L, 0x5A1F5C1F5L, 0x0B58158ADL, 0xE712053FBL,
            0x5352DDB25L, 0x414B98EA0L, 0x74F89F546L, 0x38A56B3C3L, 0x38DB0DC17L, 0xAA016A755L,
            0xDC72366F5L, 0x0CEE93D75L, 0xB2FE7A56BL, 0xA847ED390L, 0x8713EF88CL, 0xA217CC861L,
            0x8BCA25D7BL, 0x455526818L, 0xEA3A7A180L, 0xA9536E5E0L, 0x9B64A1975L, 0x5BFC756BCL,
            0x046AA169BL, 0x53A17F76FL, 0x4D6815274L, 0xCCA9CF3F6L, 0x4013FCB8BL, 0x3D26CDFA5L,
            0x5786231F7L, 0x7D4AB09ABL, 0x960B5FFBCL, 0x8914DF0D4L, 0x2FC6F2213L, 0xAC235637EL,
            0x151B28ED3L, 0x46F79B6DBL, 0x1382E0C9FL, 0x53ABF983AL, 0x383C47ADEL, 0x3FCF88978L,
            0xEB9079DF7L, 0x09AF0714DL, 0xDA19D1BB7L, 0x9A02749F8L, 0x1C62DAB9BL, 0x1A137E44BL,
            0x2867718C7L, 0x35815525BL, 0x7CD35C550L, 0x2164F73A0L, 0xE8B772FE0L,
        };

        /// <summary>
        /// Where each bit goes, from the highest bit down, as (column, row) in the 8 x 8 black square
        /// (row 0 at the top; data cells are 1 to 6): one quarter of 9 bits, then the same quarter
        /// turned a quarter turn clockwise three times, so turning the tag turns the code.
        /// </summary>
        public static readonly Vector2Int[] BitCells = MakeBitCells();

        /// <summary>
        /// The tag's picture, <see cref="Cells"/> x <see cref="Cells"/>, true = white, [row, column] with
        /// row 0 at the top, as printed.
        /// </summary>
        public static bool[,] Pattern(int id)
        {
            if (id < 0 || id >= Codes.Length)
            {
                throw new System.ArgumentOutOfRangeException(nameof(id), "36h11 tag ids are 0 to " + (Codes.Length - 1) + ", not " + id + ".");
            }
            var white = new bool[Cells, Cells];
            for (int row = 0; row < Cells; row++)
            {
                for (int column = 0; column < Cells; column++)
                {
                    // The outer ring is the white edge; the next ring is the black border.
                    white[row, column] = row == 0 || column == 0 || row == Cells - 1 || column == Cells - 1;
                }
            }
            long code = Codes[id];
            for (int bit = 0; bit < 36; bit++)
            {
                Vector2Int cell = BitCells[bit];
                white[cell.y + 1, cell.x + 1] = ((code >> (35 - bit)) & 1) == 1;
            }
            return white;
        }

        /// <summary>The tag's picture as a small texture (one pixel per cell, kept sharp), its top row at the top.</summary>
        public static Texture2D Texture(int id)
        {
            bool[,] white = Pattern(id);
            var texture = new Texture2D(Cells, Cells, TextureFormat.RGB24, false)
            {
                name = "AprilTag 36h11 id " + id,
                filterMode = FilterMode.Point,
                wrapMode = TextureWrapMode.Clamp,
            };
            for (int row = 0; row < Cells; row++)
            {
                for (int column = 0; column < Cells; column++)
                {
                    // Texture rows count up from the bottom.
                    texture.SetPixel(column, Cells - 1 - row, white[row, column] ? Color.white : Color.black);
                }
            }
            texture.Apply();
            return texture;
        }

        private static Vector2Int[] MakeBitCells()
        {
            Vector2Int[] quarter =
            {
                new Vector2Int(1, 1), new Vector2Int(2, 1), new Vector2Int(3, 1), new Vector2Int(4, 1), new Vector2Int(5, 1),
                new Vector2Int(2, 2), new Vector2Int(3, 2), new Vector2Int(4, 2), new Vector2Int(3, 3),
            };
            var cells = new Vector2Int[36];
            for (int turn = 0; turn < 4; turn++)
            {
                for (int i = 0; i < 9; i++)
                {
                    cells[turn * 9 + i] = quarter[i];
                    quarter[i] = new Vector2Int(7 - quarter[i].y, quarter[i].x);
                }
            }
            return cells;
        }
    }
}
