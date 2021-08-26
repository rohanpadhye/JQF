package edu.berkeley.cs.jqf.examples.kaitai;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class PngKaitaiGenerator extends AbstractKaitaiGenerator {
    protected void populate(SourceOfRandomness random) {
        new Png(new KaitaiStream(buf, random));
    }

    public enum ColorType {
        GREYSCALE(0),
        TRUECOLOR(2),
        INDEXED(3),
        GREYSCALE_ALPHA(4),
        TRUECOLOR_ALPHA(6);

        private final long id;

        ColorType(long id) {
            this.id = id;
        }

        public long id() {
            return id;
        }

        private static final Map<Long, ColorType> byId = new HashMap<Long, ColorType>(5);

        static {
            for (ColorType e : ColorType.values())
                byId.put(e.id(), e);
        }

        public static ColorType byId(long id) {
            return byId.get(id);
        }

        public static long[] ids() {
            return Stream.of(ColorType.values()).mapToLong((e) -> e.id()).toArray();
        }
    }

    public enum PhysUnit {
        UNKNOWN(0),
        METER(1);

        private final long id;

        PhysUnit(long id) {
            this.id = id;
        }

        public long id() {
            return id;
        }

        private static final Map<Long, PhysUnit> byId = new HashMap<Long, PhysUnit>(2);

        static {
            for (PhysUnit e : PhysUnit.values())
                byId.put(e.id(), e);
        }

        public static PhysUnit byId(long id) {
            return byId.get(id);
        }

        public static long[] ids() {
            return Stream.of(PhysUnit.values()).mapToLong((e) -> e.id()).toArray();
        }
    }

    public static class Png extends KaitaiStruct {

        public Png(KaitaiStream _io) {
            super(_io);
            this._root = this;
            _write();
        }

        public Png(KaitaiStream _io, KaitaiStruct _parent) {
            super(_io);
            this._parent = _parent;
            this._root = this;
            _write();
        }

        public Png(KaitaiStream _io, KaitaiStruct _parent, Png _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _write();
        }

        private void _write() {
            this.magic = this._io.ensureFixedContents(new byte[]{-119, 80, 78, 71, 13, 10, 26, 10});
            this.ihdrLen = this._io.ensureFixedContents(new byte[]{0, 0, 0, 13});
            this.ihdrType = this._io.ensureFixedContents(new byte[]{73, 72, 68, 82});
            this.ihdr = new IhdrChunk(this._io, this, _root);
            this.ihdrCrc = this._io.writeBytes(4);
            this.chunks = new ArrayList<Chunk>();
            while (!this._io.isEof()) {
                Chunk chunk;
                try {
                    chunk = new Chunk(this._io, this, _root);
                    this.chunks.add(chunk);
                    if (chunk.type().equals("IEND")) {
                        break;
                    }
                } catch (BufferOverflowException e) {
                    throw e;
                }
            }
        }

        public static class Rgb extends KaitaiStruct {

            public Rgb(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public Rgb(KaitaiStream _io, PlteChunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public Rgb(KaitaiStream _io, PlteChunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.r = this._io.writeU1();
                this.g = this._io.writeU1();
                this.b = this._io.writeU1();
            }

            private int r;
            private int g;
            private int b;
            private Png _root;
            private Png.PlteChunk _parent;

            public int r() {
                return r;
            }

            public int g() {
                return g;
            }

            public int b() {
                return b;
            }

            public Png _root() {
                return _root;
            }

            public Png.PlteChunk _parent() {
                return _parent;
            }
        }

        public static class Chunk extends KaitaiStruct {
            public Chunk(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public Chunk(KaitaiStream _io, Png _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public Chunk(KaitaiStream _io, Png _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.len = this._io.writeU4be(0, this._io.buf.remaining());
                byte[][] types = {
                        "IEND".getBytes(),
                        "iTXt".getBytes(),
                        "gAMA".getBytes(),
                        "tIME".getBytes(),
                        "PLTE".getBytes(),
                        "bKGD".getBytes(),
                        "pHYs".getBytes(),
                        "tEXt".getBytes(),
                        "cHRM".getBytes(),
                        "sRGB".getBytes(),
                        "zTXt".getBytes(),
                        "IDAT".getBytes(),
                };
                this.type = new String(this._io.writeBytesOneOfFixedSize(4, types), Charset.forName("UTF-8"));
                // System.out.println(String.format("Chunk %s of length %d", type, len));
                switch (type()) {
                    case "iTXt": {
                        this._raw_body = this._io.getSlice(len());
                        KaitaiStream _io__raw_body = new KaitaiStream(_raw_body, _io.random);
                        this.body = new InternationalTextChunk(_io__raw_body, this, _root);
                        break;
                    }
                    case "gAMA": {
                        this._raw_body = this._io.getSlice(len());
                        KaitaiStream _io__raw_body = new KaitaiStream(_raw_body, _io.random);
                        this.body = new GamaChunk(_io__raw_body, this, _root);
                        break;
                    }
                    case "tIME": {
                        this._raw_body = this._io.getSlice(len());
                        KaitaiStream _io__raw_body = new KaitaiStream(_raw_body, _io.random);
                        this.body = new TimeChunk(_io__raw_body, this, _root);
                        break;
                    }
                    case "PLTE": {
                        this._raw_body = this._io.getSlice(len());
                        KaitaiStream _io__raw_body = new KaitaiStream(_raw_body, _io.random);
                        this.body = new PlteChunk(_io__raw_body, this, _root);
                        break;
                    }
                    case "bKGD": {
                        this._raw_body = this._io.getSlice(len());
                        KaitaiStream _io__raw_body = new KaitaiStream(_raw_body, _io.random);
                        this.body = new BkgdChunk(_io__raw_body, this, _root);
                        break;
                    }
                    case "pHYs": {
                        this._raw_body = this._io.getSlice(len());
                        KaitaiStream _io__raw_body = new KaitaiStream(_raw_body, _io.random);
                        this.body = new PhysChunk(_io__raw_body, this, _root);
                        break;
                    }
                    case "tEXt": {
                        this._raw_body = this._io.getSlice(len());
                        KaitaiStream _io__raw_body = new KaitaiStream(_raw_body, _io.random);
                        this.body = new TextChunk(_io__raw_body, this, _root);
                        break;
                    }
                    case "cHRM": {
                        this._raw_body = this._io.getSlice(len());
                        KaitaiStream _io__raw_body = new KaitaiStream(_raw_body, _io.random);
                        this.body = new ChrmChunk(_io__raw_body, this, _root);
                        break;
                    }
                    case "sRGB": {
                        this._raw_body = this._io.getSlice(len());
                        KaitaiStream _io__raw_body = new KaitaiStream(_raw_body, _io.random);
                        this.body = new SrgbChunk(_io__raw_body, this, _root);
                        break;
                    }
                    case "zTXt": {
                        this._raw_body = this._io.getSlice(len());
                        KaitaiStream _io__raw_body = new KaitaiStream(_raw_body, _io.random);
                        this.body = new CompressedTextChunk(_io__raw_body, this, _root);
                        break;
                    }
                    default: {
                        this.body = this._io.writeBytes(len());
                        break;
                    }
                }
                this.crc = this._io.writeBytes(4);
            }

            private long len;
            private String type;
            private Object body;
            private byte[] crc;
            private Png _root;
            private Png _parent;
            private ByteBuffer _raw_body;

            public long len() {
                return len;
            }

            public String type() {
                return type;
            }

            public Object body() {
                return body;
            }

            public byte[] crc() {
                return crc;
            }

            public Png _root() {
                return _root;
            }

            public Png _parent() {
                return _parent;
            }

            public byte[] _raw_body() {
                return _raw_body.array();
            }
        }

        public static class BkgdIndexed extends KaitaiStruct {
            public BkgdIndexed(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public BkgdIndexed(KaitaiStream _io, BkgdChunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public BkgdIndexed(KaitaiStream _io, BkgdChunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.paletteIndex = this._io.writeU1();
            }

            private int paletteIndex;
            private Png _root;
            private Png.BkgdChunk _parent;

            public int paletteIndex() {
                return paletteIndex;
            }

            public Png _root() {
                return _root;
            }

            public Png.BkgdChunk _parent() {
                return _parent;
            }
        }

        public static class Point extends KaitaiStruct {

            public Point(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public Point(KaitaiStream _io, ChrmChunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public Point(KaitaiStream _io, ChrmChunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.xInt = this._io.writeU4be();
                this.yInt = this._io.writeU4be();
            }

            private Double x;

            public Double x() {
                if (this.x != null)
                    return this.x;
                double _tmp = (double) ((xInt() / 100000.0));
                this.x = _tmp;
                return this.x;
            }

            private Double y;

            public Double y() {
                if (this.y != null)
                    return this.y;
                double _tmp = (double) ((yInt() / 100000.0));
                this.y = _tmp;
                return this.y;
            }

            private long xInt;
            private long yInt;
            private Png _root;
            private Png.ChrmChunk _parent;

            public long xInt() {
                return xInt;
            }

            public long yInt() {
                return yInt;
            }

            public Png _root() {
                return _root;
            }

            public Png.ChrmChunk _parent() {
                return _parent;
            }
        }

        public static class BkgdGreyscale extends KaitaiStruct {

            public BkgdGreyscale(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public BkgdGreyscale(KaitaiStream _io, BkgdChunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public BkgdGreyscale(KaitaiStream _io, BkgdChunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.value = this._io.writeU2be();
            }

            private int value;
            private Png _root;
            private Png.BkgdChunk _parent;

            public int value() {
                return value;
            }

            public Png _root() {
                return _root;
            }

            public Png.BkgdChunk _parent() {
                return _parent;
            }
        }

        public static class ChrmChunk extends KaitaiStruct {

            public ChrmChunk(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public ChrmChunk(KaitaiStream _io, Chunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public ChrmChunk(KaitaiStream _io, Chunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.whitePoint = new Point(this._io, this, _root);
                this.red = new Point(this._io, this, _root);
                this.green = new Point(this._io, this, _root);
                this.blue = new Point(this._io, this, _root);
            }

            private Point whitePoint;
            private Point red;
            private Point green;
            private Point blue;
            private Png _root;
            private Png.Chunk _parent;

            public Point whitePoint() {
                return whitePoint;
            }

            public Point red() {
                return red;
            }

            public Point green() {
                return green;
            }

            public Point blue() {
                return blue;
            }

            public Png _root() {
                return _root;
            }

            public Png.Chunk _parent() {
                return _parent;
            }
        }

        public static class IhdrChunk extends KaitaiStruct {

            public IhdrChunk(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public IhdrChunk(KaitaiStream _io, Png _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public IhdrChunk(KaitaiStream _io, Png _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.width = this._io.writeU4be(0, this._io.buf.remaining());
                this.height = this._io.writeU4be(0, this._io.buf.remaining());
                this.bitDepth = this._io.writeU1OneOf(1, 2, 4, 8, 16);
                this.colorType = ColorType.byId(this._io.writeU1OneOf(ColorType.ids()));
                this.compressionMethod = this._io.writeU1OneOf(0);
                this.filterMethod = this._io.writeU1value((byte) 0);
                this.interlaceMethod = this._io.writeU1value((byte) 0);
            }

            private long width;
            private long height;
            private int bitDepth;
            private ColorType colorType;
            private int compressionMethod;
            private int filterMethod;
            private int interlaceMethod;
            private Png _root;
            private Png _parent;

            public long width() {
                return width;
            }

            public long height() {
                return height;
            }

            public int bitDepth() {
                return bitDepth;
            }

            public ColorType colorType() {
                return colorType;
            }

            public int compressionMethod() {
                return compressionMethod;
            }

            public int filterMethod() {
                return filterMethod;
            }

            public int interlaceMethod() {
                return interlaceMethod;
            }

            public Png _root() {
                return _root;
            }

            public Png _parent() {
                return _parent;
            }
        }

        public static class PlteChunk extends KaitaiStruct {

            public PlteChunk(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public PlteChunk(KaitaiStream _io, Chunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public PlteChunk(KaitaiStream _io, Chunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.entries = new ArrayList<Rgb>();
                while (!this._io.isEof()) {
                    this.entries.add(new Rgb(this._io, this, _root));
                }
            }

            private ArrayList<Rgb> entries;
            private Png _root;
            private Png.Chunk _parent;

            public ArrayList<Rgb> entries() {
                return entries;
            }

            public Png _root() {
                return _root;
            }

            public Png.Chunk _parent() {
                return _parent;
            }
        }

        public static class SrgbChunk extends KaitaiStruct {

            public enum Intent {
                PERCEPTUAL(0),
                RELATIVE_COLORIMETRIC(1),
                SATURATION(2),
                ABSOLUTE_COLORIMETRIC(3);

                private final long id;

                Intent(long id) {
                    this.id = id;
                }

                public long id() {
                    return id;
                }

                private static final Map<Long, Intent> byId = new HashMap<Long, Intent>(4);

                static {
                    for (Intent e : Intent.values())
                        byId.put(e.id(), e);
                }

                public static Intent byId(long id) {
                    return byId.get(id);
                }

                public static long[] ids() {
                    return Stream.of(Intent.values()).mapToLong((e) -> e.id()).toArray();
                }

            }

            public SrgbChunk(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public SrgbChunk(KaitaiStream _io, Chunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public SrgbChunk(KaitaiStream _io, Chunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.renderIntent = Intent.byId(this._io.writeU1OneOf(Intent.ids()));
            }

            private Intent renderIntent;
            private Png _root;
            private Png.Chunk _parent;

            public Intent renderIntent() {
                return renderIntent;
            }

            public Png _root() {
                return _root;
            }

            public Png.Chunk _parent() {
                return _parent;
            }
        }

        public static class CompressedTextChunk extends KaitaiStruct {

            public CompressedTextChunk(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public CompressedTextChunk(KaitaiStream _io, Chunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public CompressedTextChunk(KaitaiStream _io, Chunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.keyword = new String(this._io.writeBytesTerm(0, false, true, true), Charset.forName("UTF-8"));
                this.compressionMethod = this._io.writeU1();
                this._raw_textDatastream = this._io.getSliceFull();
                this.textDatastream = new KaitaiStream(_raw_textDatastream, _io.random).processZlib();
            }

            private String keyword;
            private int compressionMethod;
            private byte[] textDatastream;
            private Png _root;
            private Png.Chunk _parent;
            private ByteBuffer _raw_textDatastream;

            public String keyword() {
                return keyword;
            }

            public int compressionMethod() {
                return compressionMethod;
            }

            public byte[] textDatastream() {
                return textDatastream;
            }

            public Png _root() {
                return _root;
            }

            public Png.Chunk _parent() {
                return _parent;
            }

            public byte[] _raw_textDatastream() {
                return _raw_textDatastream.array();
            }
        }

        public static class BkgdTruecolor extends KaitaiStruct {

            public BkgdTruecolor(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public BkgdTruecolor(KaitaiStream _io, BkgdChunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public BkgdTruecolor(KaitaiStream _io, BkgdChunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.red = this._io.writeU2be();
                this.green = this._io.writeU2be();
                this.blue = this._io.writeU2be();
            }

            private int red;
            private int green;
            private int blue;
            private Png _root;
            private Png.BkgdChunk _parent;

            public int red() {
                return red;
            }

            public int green() {
                return green;
            }

            public int blue() {
                return blue;
            }

            public Png _root() {
                return _root;
            }

            public Png.BkgdChunk _parent() {
                return _parent;
            }
        }

        public static class GamaChunk extends KaitaiStruct {

            public GamaChunk(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public GamaChunk(KaitaiStream _io, Chunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public GamaChunk(KaitaiStream _io, Chunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.gammaInt = this._io.writeU4be();
            }

            private Double gammaRatio;

            public Double gammaRatio() {
                if (this.gammaRatio != null)
                    return this.gammaRatio;
                double _tmp = (double) ((100000.0 / gammaInt()));
                this.gammaRatio = _tmp;
                return this.gammaRatio;
            }

            private long gammaInt;
            private Png _root;
            private Png.Chunk _parent;

            public long gammaInt() {
                return gammaInt;
            }

            public Png _root() {
                return _root;
            }

            public Png.Chunk _parent() {
                return _parent;
            }
        }

        public static class BkgdChunk extends KaitaiStruct {

            public BkgdChunk(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public BkgdChunk(KaitaiStream _io, Chunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public BkgdChunk(KaitaiStream _io, Chunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                switch (_root.ihdr().colorType()) {
                    case GREYSCALE_ALPHA: {
                        this.bkgd = new BkgdGreyscale(this._io, this, _root);
                        break;
                    }
                    case INDEXED: {
                        this.bkgd = new BkgdIndexed(this._io, this, _root);
                        break;
                    }
                    case GREYSCALE: {
                        this.bkgd = new BkgdGreyscale(this._io, this, _root);
                        break;
                    }
                    case TRUECOLOR_ALPHA: {
                        this.bkgd = new BkgdTruecolor(this._io, this, _root);
                        break;
                    }
                    case TRUECOLOR: {
                        this.bkgd = new BkgdTruecolor(this._io, this, _root);
                        break;
                    }
                }
            }

            private KaitaiStruct bkgd;
            private Png _root;
            private Png.Chunk _parent;

            public KaitaiStruct bkgd() {
                return bkgd;
            }

            public Png _root() {
                return _root;
            }

            public Png.Chunk _parent() {
                return _parent;
            }
        }

        public static class PhysChunk extends KaitaiStruct {

            public PhysChunk(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public PhysChunk(KaitaiStream _io, Chunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public PhysChunk(KaitaiStream _io, Chunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.pixelsPerUnitX = this._io.writeU4be();
                this.pixelsPerUnitY = this._io.writeU4be();
                this.unit = PhysUnit.byId(this._io.writeU1OneOf(PhysUnit.ids()));
            }

            private long pixelsPerUnitX;
            private long pixelsPerUnitY;
            private PhysUnit unit;
            private Png _root;
            private Png.Chunk _parent;

            public long pixelsPerUnitX() {
                return pixelsPerUnitX;
            }

            public long pixelsPerUnitY() {
                return pixelsPerUnitY;
            }

            public PhysUnit unit() {
                return unit;
            }

            public Png _root() {
                return _root;
            }

            public Png.Chunk _parent() {
                return _parent;
            }
        }

        public static class InternationalTextChunk extends KaitaiStruct {

            public InternationalTextChunk(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public InternationalTextChunk(KaitaiStream _io, Chunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public InternationalTextChunk(KaitaiStream _io, Chunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.keyword = new String(this._io.writeBytesTerm(0, false, true, true), Charset.forName("UTF-8"));
                this.compressionFlag = this._io.writeU1();
                this.compressionMethod = this._io.writeU1();
                this.languageTag = new String(this._io.writeBytesTerm(0, false, true, true), Charset.forName("ASCII"));
                this.translatedKeyword = new String(this._io.writeBytesTerm(0, false, true, true), Charset.forName("UTF-8"));
                this.text = new String(this._io.writeBytesFull(), Charset.forName("UTF-8"));
            }

            private String keyword;
            private int compressionFlag;
            private int compressionMethod;
            private String languageTag;
            private String translatedKeyword;
            private String text;
            private Png _root;
            private Png.Chunk _parent;

            public String keyword() {
                return keyword;
            }

            public int compressionFlag() {
                return compressionFlag;
            }

            public int compressionMethod() {
                return compressionMethod;
            }

            public String languageTag() {
                return languageTag;
            }

            public String translatedKeyword() {
                return translatedKeyword;
            }

            public String text() {
                return text;
            }

            public Png _root() {
                return _root;
            }

            public Png.Chunk _parent() {
                return _parent;
            }
        }

        public static class TextChunk extends KaitaiStruct {

            public TextChunk(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public TextChunk(KaitaiStream _io, Chunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public TextChunk(KaitaiStream _io, Chunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.keyword = new String(this._io.writeBytesTerm(0, false, true, true), Charset.forName("iso8859-1"));
                this.text = new String(this._io.writeBytesFull(), Charset.forName("iso8859-1"));
            }

            private String keyword;
            private String text;
            private Png _root;
            private Png.Chunk _parent;

            public String keyword() {
                return keyword;
            }

            public String text() {
                return text;
            }

            public Png _root() {
                return _root;
            }

            public Png.Chunk _parent() {
                return _parent;
            }
        }

        public static class TimeChunk extends KaitaiStruct {

            public TimeChunk(KaitaiStream _io) {
                super(_io);
                _write();
            }

            public TimeChunk(KaitaiStream _io, Chunk _parent) {
                super(_io);
                this._parent = _parent;
                _write();
            }

            public TimeChunk(KaitaiStream _io, Chunk _parent, Png _root) {
                super(_io);
                this._parent = _parent;
                this._root = _root;
                _write();
            }

            private void _write() {
                this.year = this._io.writeU2be();
                this.month = this._io.writeU1();
                this.day = this._io.writeU1();
                this.hour = this._io.writeU1();
                this.minute = this._io.writeU1();
                this.second = this._io.writeU1();
            }

            private int year;
            private int month;
            private int day;
            private int hour;
            private int minute;
            private int second;
            private Png _root;
            private Png.Chunk _parent;

            public int year() {
                return year;
            }

            public int month() {
                return month;
            }

            public int day() {
                return day;
            }

            public int hour() {
                return hour;
            }

            public int minute() {
                return minute;
            }

            public int second() {
                return second;
            }

            public Png _root() {
                return _root;
            }

            public Png.Chunk _parent() {
                return _parent;
            }
        }

        private byte[] magic;
        private byte[] ihdrLen;
        private byte[] ihdrType;
        private IhdrChunk ihdr;
        private byte[] ihdrCrc;
        private ArrayList<Chunk> chunks;
        private Png _root;
        private KaitaiStruct _parent;

        public byte[] magic() {
            return magic;
        }

        public byte[] ihdrLen() {
            return ihdrLen;
        }

        public byte[] ihdrType() {
            return ihdrType;
        }

        public IhdrChunk ihdr() {
            return ihdr;
        }

        public byte[] ihdrCrc() {
            return ihdrCrc;
        }

        public ArrayList<Chunk> chunks() {
            return chunks;
        }

        public Png _root() {
            return _root;
        }

        public KaitaiStruct _parent() {
            return _parent;
        }
    }
}
