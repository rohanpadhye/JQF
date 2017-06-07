/*  -*- Last-Edit:  Mon Dec  7 10:31:51 1992 by Tarak S. Goradia; -*-
* ported to Java 
* */
package benchmarks.wise;

import benchmarks.wise.driver.Driver;
import benchmarks.wise.driver.Driver;

public class Replace {


final int MYMAX = 20;

final int MAXSTR = 100;
final int MAXPAT = 100;

final char ENDSTR  = '\0';
    final char ESCAPE = '@';
final char CLOSURE = '*';
final char BOL =    '%';
final char EOL  =   '$';
final char ANY =    '?';
final char CCL   =  '[';
final char CCLEND = ']';
final char NEGATE  = '^';
final char NCCL  =  '!';
final char LITCHAR = 'c';
final int DITTO  = -1;
final char DASH =   '-';

final char TAB =    9;
final char NEWLINE = 10;

final int CLOSIZE = 1;

//    typedef char	character;
//    typedef char string[MAXSTR];

    boolean getline(char[] s, int maxsize) {
        char[] result;
        int i;
        char c;

        for(i=0;i<MYMAX-1;i++) {
            c = Driver.readCharacter();
            s[i] = c;
        }
        s[i] = '\0';
        return true;
    }

    boolean addstr(char c, char[] outset, int[] j, int maxset)
    {
        boolean 	result;
        if (j[0] >= maxset)
            result = false;
        else {
            outset[j[0]] = c;
            j[0] = j[0] + 1;
            result = true;
        }
        return result;
    }

    char esc(char[] s, int[] i) {
        char	result;
        if (s[i[0]] != ESCAPE)
            result = s[i[0]];
        else
        if (s[i[0] + 1] == ENDSTR)
            result = ESCAPE;
        else
        {
            i[0] = i[0] + 1;
            if (s[i[0]] == 'n')
                result = NEWLINE;
            else
            if (s[i[0]] == 't')
                result = TAB;
            else
                result = s[i[0]];
        }
        return result;
    }

    void dodash(char delim, char[] src, int[] i, char[] dest, int[] j, int maxset)
    {
        int	k;
        boolean 	junk;
        char	escjunk;

        while ((src[i[0]] != delim) && (src[i[0]] != ENDSTR))
        {
        if (src[i[0] - 1] == ESCAPE) {
            escjunk = esc(src, i);
            junk = addstr(escjunk, dest, j, maxset);
        } else
            if (src[i[0]] != DASH)
            junk = addstr(src[i[0]], dest, j, maxset);
            else if (j[0] <= 1 || src[i[0] + 1] == ENDSTR)
            junk = addstr(DASH, dest, j, maxset);
            else if ((isalnum(src[i[0] - 1])) && (isalnum(src[i[0] + 1]))
            && (src[i[0] - 1] <= src[i[0] + 1]))
            {
                for (k = src[i[0]-1]+1; k<=src[i[0]+1]; k++)
                {
                junk = addstr((char)k, dest, j, maxset);
                }
                i[0] = i[0] + 1;
            }
            else
            junk = addstr(DASH, dest, j, maxset);
        i[0] = i[0] + 1;
        }
    }

    private boolean isalnum(char c) {
        return ((c >='a' && c <='z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'));
    }

    boolean  getccl(char[] arg, int[] i, char[] pat, int[] j) {
        int	jstart;
        boolean 	junk;

        i[0] = i[0] + 1;
        if (arg[i[0]] == NEGATE) {
            junk = addstr(NCCL, pat, j, MAXPAT);
            i[0] = i[0] + 1;
        } else
            junk = addstr(CCL, pat, j, MAXPAT);
        jstart = j[0];
        junk = addstr((char)0, pat, j, MAXPAT);
        dodash(CCLEND, arg, i, pat, j, MAXPAT);
        pat[jstart] = (char)(j[0] - jstart - 1);
        return (arg[i[0]] == CCLEND);
    }

    void stclose(char[] pat, int[] j, int lastj) {
        int[]	jt = new int[1];
        int	jp;
        boolean junk;


        for (jp = j[0] - 1; jp >= lastj ; jp--)
        {
            jt[0] = jp + CLOSIZE;
            junk = addstr(pat[jp], pat, jt, MAXPAT);
        }
        j[0] = j[0] + CLOSIZE;
        pat[lastj] = CLOSURE;
    }

    boolean in_set_2(char c) {
        return (c == BOL || c == EOL || c == CLOSURE);
    }

    boolean in_pat_set(char c){
      return (   c == LITCHAR || c == BOL  || c == EOL || c == ANY
              || c == CCL     || c == NCCL || c == CLOSURE);
    }

    int makepat(char[] arg, int start, char delim, char[] pat) {
        int	result;
        int	lastj, lj;
        boolean 	done, junk;
        boolean 	getres;
        char	escjunk;

        int[] j = new int[1];
        j[0] = 0;
        int[] i = new int[1];
        i[0] = start;
        lastj = 0;
        done = false;
        while ((!done) && (arg[i[0]] != delim) && (arg[i[0]] != ENDSTR)) {
            lj = j[0];
            if ((arg[i[0]] == ANY))
                junk = addstr(ANY, pat, j, MAXPAT);
            else if ((arg[i[0]] == BOL) && (i[0] == start))
                junk = addstr(BOL, pat, j, MAXPAT);
            else if ((arg[i[0]] == EOL) && (arg[i[0]+1] == delim))
                junk = addstr(EOL, pat, j, MAXPAT);
            else if ((arg[i[0]] == CCL))
            {
                getres = getccl(arg, i, pat, j);
                done = (getres == false);
            }
            else if ((arg[i[0]] == CLOSURE) && (i[0] > start))
            {
                lj = lastj;
                if (in_set_2(pat[lj]))
                    done = true;
                else
                    stclose(pat, j, lastj);
            }
            else
            {
                junk = addstr(LITCHAR, pat, j, MAXPAT);
                escjunk = esc(arg, i);
                junk = addstr(escjunk, pat, j, MAXPAT);
            }
            lastj = lj;
            if ((!done))
                i[0] = i[0] + 1;
        }
        junk = addstr(ENDSTR, pat, j, MAXPAT);
        if ((done) || (arg[i[0]] != delim))
            result = 0;
        else
        if ((!junk))
            result = 0;
        else
            result = i[0];
        return result;
    }

    boolean getpat(char[] arg, char[] pat) {
        int	makeres;

        makeres = makepat(arg, 0, ENDSTR, pat);
        return (makeres > 0);
    }

    int makesub(char[] arg, int from, char delim, char[] sub) {
        int  result;
        int[]	i = new int[1];
        boolean 	junk;
        char	escjunk;

        int[] j = new int[1];
        j[0] = 0;
        i[0] = from;
        while ((arg[i[0]] != delim) && (arg[i[0]] != ENDSTR)) {
            if ((arg[i[0]] == ('&')))
                junk = addstr((char)DITTO, sub, j, MAXPAT);
            else {
                escjunk = esc(arg, i);
                junk = addstr(escjunk, sub, j, MAXPAT);
            }
            i[0] = i[0] + 1;
        }
        if (arg[i[0]] != delim)
            result = 0;
        else {
            junk = addstr(ENDSTR, sub, j, MAXPAT);
            if ((!junk))
                result = 0;
            else
                result = i[0];
        }
        return result;
    }

    boolean getsub(char[] arg, char[] sub) {
        int	makeres;

        makeres = makesub(arg, 0, ENDSTR, sub);
        return (makeres > 0);
    }

     boolean locate(char c, char[] pat, int offset) {
        int	i;
        boolean flag;

        flag = false;
        i = offset + pat[offset];
        while ((i > offset))
        {
        if (c == pat[i]) {
            flag = true;
            i = offset;
        } else
            i = i - 1;
        }
        return flag;
    }

    boolean omatch(char[] lin, int[] i, char[] pat, int j) {
        char	advance;
        boolean result;

        advance = (char)-1;
        if ((lin[i[0]] == ENDSTR))
        result = false;
        else
        {
        if (!in_pat_set(pat[j]))
        {
            System.err.print("in omatch: can't happen\n");
            System.exit(0);
        } else
        {
             switch (pat[j])
             {
             case LITCHAR:
             if (lin[i[0]] == pat[j + 1])
                 advance = 1;
             break ;
             case BOL:
             if (i[0] == 0)
                 advance = 0;
             break ;
             case ANY:
             if (lin[i[0]] != NEWLINE)
                 advance = 1;
             break ;
             case EOL:
             if (lin[i[0]] == NEWLINE)
                 advance = 0;
             break ;
             case CCL:
             if (locate(lin[i[0]], pat, j + 1))
                 advance = 1;
             break ;
             case NCCL:
             if ((lin[i[0]] != NEWLINE) && (!locate(lin[i[0]], pat, j+1)))
                 advance = 1;
             break ;
             default:
             Caseerror(pat[j]);
             };
         }
        }
        if ((advance >= 0))
        {
        i[0] = i[0] + advance;
        result = true;
        } else
        result = false;
        return result;
    }


    int patsize(char[] pat, int n) {
        int size=0;
        if (!in_pat_set(pat[n])) {
        System.err.print("in patsize: can't happen\n");
        System.exit(0);
        } else
        switch (pat[n])
        {
        case LITCHAR: size = 2; break;

        case BOL:  case EOL:  case ANY:
            size = 1;
            break;
        case CCL:  case NCCL:
            size = pat[n + 1] + 2;
            break ;
        case CLOSURE:
            size = CLOSIZE;
            break ;
        default:
            Caseerror(pat[n]);
        }
        return size;
    }

    int amatch(char[] lin, int offset, char[] pat, int j) {
        int	i, k=0;
        boolean 	result, done;
        int[] offseta = new int[1];
        int[] ia = new int[1];

        done = false;
        while ((!done) && (pat[j] != ENDSTR))
        if ((pat[j] == CLOSURE)) {
            j = j + patsize(pat, j);
            i = offset;
            while ((!done) && (lin[i] != ENDSTR)) {
                ia[0] = i;
            result = omatch(lin, ia, pat, j);
                i = ia[0];
            if (!result)
                done = true;
            }
            done = false;
            while ((!done) && (i >= offset)) {
            k = amatch(lin, i, pat, j + patsize(pat, j));
            if ((k >= 0))
                done = true;
            else
                i = i - 1;
            }
            offset = k;
            done = true;
        } else {
            offseta[0] = offset;
            result = omatch(lin, offseta, pat, j);
            offset = offseta[0];
            if ((!result)) {
            offset = -1;
            done = true;
            } else
            j = j + patsize(pat, j);
        }
         return offset;
    }

    void putsub(char[] lin, int s1, int s2, char[] sub) {
        int	i;
        int		j;

        i = 0;
        while ((sub[i] != ENDSTR)) {
            if ((sub[i] == DITTO))
                for (j = s1; j < s2; j++)
                {
                    System.out.print(lin[j]);
                }
            else
            {
                System.out.print(sub[i]);
            }
            i = i + 1;
        }
    }

    void subline(char[] lin, char[] pat, char[] sub) {
        int	i, lastm, m;

        lastm = -1;
        i = 0;
        while ((lin[i] != ENDSTR))
        {
            m = amatch(lin, i, pat, 0);
            if ((m >= 0) && (lastm != m)) {
                putsub(lin, i, m, sub);
                lastm = m;
            }
            if ((m == -1) || (m == i)) {
                System.out.print(lin[i]);
                i = i + 1;
            } else
                i = m;
        }
    }

    void change(char[] pat, char[] sub) {
        char[] line = new char[MAXSTR];
        boolean result;

        result = getline(line, MAXSTR);
        if ((result)) {
            subline(line, pat, sub);
        }
    }

    void main()
    {
        char[] pat = new char[MAXSTR];
        char[] sub = new char[MAXSTR];
        boolean result;
        char c;

        char[] input1 = new char[MAXSTR];
        char[] input2 = new char[MAXSTR];

       int i;
       for(i=0;i<MYMAX-1;i++) {
           c = Driver.readCharacter();
           input1[i] = c;
       }
       input1[i] = '\0';
       for(i=0;i<MYMAX-1;i++) {
           c = Driver.readCharacter();
           input2[i] = c;
       }
       input2[i] = '\0';

       result = getpat(input1, pat);
       if (!result)
       {
           System.out.print("change: illegal \"from\" pattern\n");
           System.exit(0);
       }

       result = getsub(input2, sub);
       if (!result)
       {
           System.out.print("change: illegal \"to\" string\n");
           System.exit(0);
       }

       change(pat, sub);
    }

    void  Caseerror(int n) {
        System.out.println("Missing case limb: line"+n);
        System.exit(0);
    }


    public static void main(String[] args) {
        (new Replace()).main();
        Driver.exit();

    }
}
