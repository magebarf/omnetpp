//==========================================================================
//  CMERSENNETWISTER.CC - part of
//                         OMNeT++
//              Discrete System Simulation in C++
//
// Contents:
//   class cMersenneTwister
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2004 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "cenvir.h"
#include "defs.h"
#include "util.h"
#include "crng.h"
#include "cexception.h"
#include "cconfig.h"
#include "mersennetwister.h"


/**
 * Wraps the Mersenne Twister RNG by Makoto Matsumoto and Takuji Nishimura.
 * Cycle length is 2^19937-1, and 623-dimensional equidistribution property
 * is assured.
 *
 * http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/ewhat-is-mt.html
 *
 * Actual code used is MersenneTwister.h from Richard J. Wagner,
 * v1.0, 15 May 2003, rjwagner@writeme.com.
 *
 * http://www-personal.engin.umich.edu/~wagnerr/MersenneTwister.html
 */
class SIM_API cMersenneTwister : public cRNG
{
  protected:
    MTRand rng;

  public:
    cMersenneTwister() {}
    virtual ~cMersenneTwister() {}

    /**
     * Sets up the RNG.
     */
    virtual void initialize(int runnumber, int id, cConfiguration *cfg);

    /** Random integer in the range [0,intRandMax()] */
    virtual unsigned long intRand();

    /** Maximum value that can be returned by intRand() */
    virtual unsigned long intRandMax();

    /** Random integer in [0,n], n < intRandMax() */
    virtual unsigned long intRand(unsigned long n);

    /** Random double on the [0,1) interval */
    virtual double doubleRand();

    /** Random double on the (0,1) interval */
    virtual double doubleRandNonz();

    /** Random double on the [0,1] interval */
    virtual double doubleRandIncl1();
};

Register_Class(cMersenneTwister);


void cMersenneTwister::initialize(int runnumber, int id, cConfiguration *cfg)
{
    unsigned int seed = 0; //FIXME
    rng.seed(seed);
}

unsigned long cMersenneTwister::intRand()
{
    return rng.randInt();
}

unsigned long cMersenneTwister::intRandMax()
{
    return 0xffffffffUL; // 2^32-1
}

unsigned long cMersenneTwister::intRand(unsigned long n)
{
    return rng.randInt(n);
}

double cMersenneTwister::doubleRand()
{
    return rng.randExc();
}

double cMersenneTwister::doubleRandNonz()
{
    return rng.randDblExc();
}

double cMersenneTwister::doubleRandIncl1()
{
    return rng.rand();
}


