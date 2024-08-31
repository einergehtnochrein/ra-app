package de.leckasemmel.sonde1;

import static java.lang.Math.abs;

public class MonitorCodec {

    final int[] power2 = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384};
    int yl = 34816;
    int yu = 544;

    int[] dq = {32,32,32,32,32,32};
    int[] sr = {32,32};

    int[] a = {0,0};
    int[] b = {0,0,0,0,0,0};
    int ap = 0;

    int dms = 0;
    int dml = 0;

    int td = 0;
    int[] pk = {0,0};

    final short[] dqlntab = {116, 365, 365, 116};
    final int[] WI = {-704, 14048, 14048, -704};
    final int[] FI = {0, 0xE00, 0xE00, 0};


    private int quan(int val, int[] table) {
        int i;
        for (i = 0; i < 15; i++) {
            if (val < table[i]) {
                break;
            }
        }
        return i;
    }

    private int fmult(int an, int srn) {
        int anmag = an;
        if (an <= 0) {
            anmag = (-an) & 0x1FFF;
        }
        int anexp = quan(anmag, power2) - 6;
        int anmant = anmag == 0 ? 32 : (anexp >= 0 ? anmag >> anexp : anmag << -anexp);
        int wanexp = anexp + ((srn >> 6) & 0xF) - 17;

        int wanmant = anmant * (srn & 0x3F) + 0x30;
        int retval = wanexp >= 0 ? ((wanmant << wanexp) & 0x7FFF) : wanmant >> -wanexp;
        //printf("  an={an}  srn={srn}  retval={retval}  anexp={anexp}  anmant={anmant}  wanexp={wanexp}  wanmant={wanmant}");
        return (an ^ srn) < 0 ? -retval : retval;
    }

    private int predictor_zero() {
        int sezi = 0;
        for (int i = 0; i < 6; i++) {
            sezi += fmult(b[i] >> 2, dq[i]);
        }
        return sezi;
    }

    private int predictor_pole() {
        return fmult(a[0] >> 2, sr[0]) + fmult(a[1] >> 2, sr[1]);
    }

    int step_size() {
        if (ap >= 256) {
            return yu;
        } else {
            int y = yl >> 6;
            int dif = yu - y;
            int al = ap >> 2;
            if (dif > 0) {
                y += (dif * al) >> 6;
            } else {
                y += (dif * al + 63) >> 6;
            }
            return y;
        }
    }

    int reconstruct(int I, int y) {
        boolean sign = (I & 0x02) != 0;

        int dql = dqlntab[I] + (y >> 2);

        if (dql < 0) {
            return sign ? -0x8000 : 0;
        }

        int dex = (dql >> 7) & 15;
        int dqt = 128 + (dql & 127);
        int dqr = (dqt << 7) >> (14 - dex);

        //printf(" dqln={dqlntab[I]}  y={y}  dql={dql}  dex={dex}  dqt={dqt}  dq={dq}\n");

        return sign ? dqr - 0x8000 : dqr;
    }

    private void update(byte I, int y, int dq0, int sr0, int dqsez) {
        int pk0 = dqsez < 0 ? 1 : 0;
        int mag = dq0 & 0x7FFF;

        int ylint = yl >> 15;
        int ylfrac = (yl >> 10) & 0x1F;
        int thr1 = (32 + ylfrac) << ylint;
        int thr2 = ylint > 9 ? 31 << 10 : thr1;
        int dqthr = (thr2 + (thr2 >> 1)) >> 1;
        int tr = 0;
        if (td != 0) {
            if (mag > dqthr) {
                tr = 1;
            }
        }

        yu = y + ((WI[I] - y) >> 5);

        if (yu < 544) {
            yu = 544;
        }
        if (yu > 5120) {
            yu = 5120;
        }

        yl += yu + ((-yl) >> 6);

        int a2p = 0;
        if (tr == 1) {
            a[0] = a[1] = 0;
            b[0] = b[1] = b[2] = b[3] = b[4] = b[5] = 0;
        } else {
            int pks1 = pk0 ^ pk[0];

            a2p = a[1] - (a[1] >> 7);
            if (dqsez != 0) {
                int fa1 = pks1 != 0 ? a[0] : -a[0];
                if (fa1 < -8191) {
                    a2p -= 0x100;
                } else if (fa1 >8191) {
                    a2p += 0xFF;
                } else {
                    a2p += fa1 >> 5;
                }

                if ((pk0 ^ pk[1]) != 0) {
                    if (a2p <= -12160) {
                        a2p = -12288;
                    } else if (a2p >=12416) {
                        a2p = 12288;
                    } else {
                        a2p -= 0x80;
                    }
                } else if (a2p <= -12416) {
                    a2p = -12288;
                } else if (a2p >= 12160) {
                    a2p = 12288;
                } else {
                    a2p += 0x80;
                }
            }

            a[1] = a2p;
            a[0] -= a[0] >> 8;
            if (dqsez != 0) {
                if (pks1 == 0) {
                    a[0] += 192;
                } else {
                    a[0] -= 192;
                }
            }

            int a1ul = 15360 - a2p;
            if (a[0] < -a1ul) {
                a[0] = -a1ul;
            }
            if (a[0] > a1ul) {
                a[0] = a1ul;
            }

            for (int cnt = 0; cnt < 6 ; cnt++) {
                b[cnt] -= b[cnt] >> 8;
                if ((dq0 & 0x7FFF) != 0) {
                    if ((dq0 ^ dq[cnt]) >= 0) {
                        b[cnt] += 128;
                    } else {
                        b[cnt] -= 128;
                    }
                }
            }
        }

        dq[5] = dq[4];
        dq[4] = dq[3];
        dq[3] = dq[2];
        dq[2] = dq[1];
        dq[1] = dq[0];
        if (mag == 0) {
            dq[0] = dq0 >= 0 ? 32 : -992;
        } else {
            int exp = quan(mag, power2);
            dq[0] = (exp << 6) + ((mag << 6) >> exp);
            if (dq0 < 0) {
                dq[0] -= 0x400;
            }
        }

        sr[1] = sr[0];
        if (sr0 == 0) {
            sr[0] = 32;
        } else if (sr0 > 0) {
            int exp = quan(sr0, power2);
            sr[0] = (exp << 6) + ((sr0 << 6) >> exp);
        } else if (sr0 > -32768) {
            mag = -sr0;
            int exp = quan(mag, power2);
            sr[0] = (exp << 6) + ((mag << 6) >> exp) - 0x400;
        } else {
            sr[0] = -992;
        }

        pk[1] = pk[0];
        pk[0] = pk0;

        if (tr == 1) {
            td = 0;
        } else if (a2p < -11776) {
            td = 1;
        } else {
            td = 0;
        }

        dms += (FI[I] - dms) >> 5;
        dml += ((FI[I] << 2) - dml) >> 7;

        if (tr == 1) {
            ap = 256;
        } else if (y < 1536) {
            ap += (0x200 - ap) >> 4;
        } else if (td == 1) {
            ap += (0x200 - ap) >> 4;
        } else if (abs((dms << 2) - dml) >= (dml >> 3)) {
            ap += (0x200 - ap) >> 4;
        } else {
            ap += (-ap) >> 4;
        }
    }

    int adpcm_decode (byte I) {
        I &= 0x03;			/* mask to get proper bits */
        int sezi = predictor_zero();
        int sez = sezi >> 1;
        int sei = sezi + predictor_pole();
        int se = sei >> 1;			/* se = estimated signal */

        int y = step_size();	/* adaptive quantizer step size */
        int _dq = reconstruct(I, y); /* unquantize pred diff */

        int _sr = (_dq < 0) ? (se - (_dq & 0x3FFF)) : (se + _dq); /* reconst. signal */

        int dqsez = _sr - se + sez;			/* pole prediction diff. */

        update(I, y, _dq, _sr, dqsez);

        return _sr << 2;
    }
}

