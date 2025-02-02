/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */
package com.revolsys.geometry.math;

import java.io.Serializable;

/**
 * Implements extended-precision floating-point numbers
 * which maintain 106 bits (approximately 30 decimal digits) of precision.
 * <p>
 * A DoubleDouble uses a representation containing two double-precision values.
 * A number x is represented as a pair of doubles, x.hi and x.lo,
 * such that the number represented by x is x.hi + x.lo, where
 * <pre>
 *    |x.lo| <= 0.5*ulp(x.hi)
 * </pre>
 * and ulp(y) means "unit in the last place of y".
 * The basic arithmetic operations are implemented using
 * convenient properties of IEEE-754 floating-point arithmetic.
 * <p>
 * The range of values which can be represented is the same as in IEEE-754.
 * The precision of the representable numbers
 * is twice as great as IEEE-754 double precision.
 * <p>
 * The correctness of the arithmetic algorithms relies on operations
 * being performed with standard IEEE-754 double precision and rounding.
 * This is the Java standard arithmetic model, but for performance reasons
 * Java implementations are not
 * constrained to using this standard by default.
 * Some processors (notably the Intel Pentium architecure) perform
 * floating point operations in (non-IEEE-754-standard) extended-precision.
 * A JVM implementation may choose to use the non-standard extended-precision
 * as its default arithmetic mode.
 *
 * <p>
 * The API provides both a set of value-oriented operations
 * and a set of mutating operations.
 * Value-oriented operations treat DoubleDouble values as
 * immutable; operations on them return new objects carrying the result
 * of the operation.  This provides a simple and safe semantics for
 * writing DoubleDouble expressions.  However, there is a performance
 * penalty for the object allocations required.
 * The mutable interface updates object values in-place.
 * It provides optimum memory performance, but requires
 * care to ensure that aliasing errors are not created
 * and constant values are not changed.
 * <p>
 * This implementation uses algorithms originally designed variously by
 * Knuth, Kahan, Dekker, and Linnainmaa.
 * Douglas Priest developed the first C implementation of these techniques.
 * Other more recent C++ implementation are due to Keith M. Briggs and David Bailey et al.
 *
 * <h3>References</h3>
 * <ul>
 * <li>Priest, D., <i>Algorithms for Arbitrary Precision Floating Point Arithmetic</i>,
 * in P. Kornerup and D. Matula, Eds., Proc. 10th Symposium on Computer Arithmetic,
 * IEEE Computer Society Press, Los Alamitos, Calif., 1991.
 * <li>Yozo Hida, Xiaoye S. Li and David H. Bailey,
 * <i>Quad-Double Arithmetic: Algorithms, Implementation, and Application</i>,
 * manuscript, Oct 2000; Lawrence Berkeley National Laboratory Report BNL-46996.
 * <li>David Bailey, <i>High Precision Software Directory</i>;
 * <tt>http://crd.lbl.gov/~dhbailey/mpdist/index.html</tt>
 * </ul>
 *
 *
 * @author Martin Davis
 *
 */
public final class DD implements Serializable, Comparable, Cloneable {
  /**
   * The value nearest to the constant e (the natural logarithm base).
   */
  public static final DD E = new DD(2.718281828459045091e+00, 1.445646891729250158e-16);

  /**
   * The smallest representable relative difference between two {link @ DoubleDouble} values
   */
  public static final double EPS = 1.23259516440783e-32; /* = 2^-106 */

  private static final int MAX_PRINT_DIGITS = 32;

  /**
   * A value representing the result of an operation which does not return a valid number.
   */
  public static final DD NaN = new DD(Double.NaN, Double.NaN);

  private static final DD ONE = DD.valueOf(1.0);

  /**
   * The value nearest to the constant Pi.
   */
  public static final DD PI = new DD(3.141592653589793116e+00, 1.224646799147353207e-16);

  /**
   * The value nearest to the constant Pi / 2.
   */
  public static final DD PI_2 = new DD(1.570796326794896558e+00, 6.123233995736766036e-17);

  private static final String SCI_NOT_EXPONENT_CHAR = "E";

  private static final String SCI_NOT_ZERO = "0.0E0";

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  /**
   * The value to split a double-precision value on during multiplication
   */
  private static final double SPLIT = 134217729.0D; // 2^27+1, for IEEE double

  private static final DD TEN = DD.valueOf(10.0);

  /**
   * The value nearest to the constant 2 * Pi.
   */
  public static final DD TWO_PI = new DD(6.283185307179586232e+00, 2.449293598294706414e-16);

  /**
   * Creates a new DoubleDouble with the value of the argument.
   *
   * @param dd the DoubleDouble value to copy
   * @return a copy of the input value
   */
  public static DD copy(final DD dd) {
    return new DD(dd);
  }

  /**
   * Determines the decimal magnitude of a number.
   * The magnitude is the exponent of the greatest power of 10 which is less than
   * or equal to the number.
   *
   * @param x the number to find the magnitude of
   * @return the decimal magnitude of x
   */
  private static int magnitude(final double x) {
    final double xAbs = Math.abs(x);
    final double xLog10 = Math.log(xAbs) / Math.log(10);
    int xMag = (int)Math.floor(xLog10);
    /**
     * Since log computation is inexact, there may be an off-by-one error
     * in the computed magnitude.
     * Following tests that magnitude is correct, and adjusts it if not
     */
    final double xApprox = Math.pow(10, xMag);
    if (xApprox * 10 <= xAbs) {
      xMag += 1;
    }

    return xMag;
  }

  private static DD newNaN() {
    return new DD(Double.NaN, Double.NaN);
  }

  /**
   * Converts a string representation of a real number into a DoubleDouble value.
   * The format accepted is similar to the standard Java real number syntax.
   * It is defined by the following regular expression:
   * <pre>
   * [<tt>+</tt>|<tt>-</tt>] {<i>digit</i>} [ <tt>.</tt> {<i>digit</i>} ] [ ( <tt>e</tt> | <tt>E</tt> ) [<tt>+</tt>|<tt>-</tt>] {<i>digit</i>}+
   * <pre>
   *
   * @param str the string to parse
   * @return the value of the parsed number
   * @throws NumberFormatException if <tt>str</tt> is not a valid representation of a number
   */
  public static DD parse(final String str) throws NumberFormatException {
    int i = 0;
    final int strlen = str.length();

    // skip leading whitespace
    while (Character.isWhitespace(str.charAt(i))) {
      i++;
    }

    // check for sign
    boolean isNegative = false;
    if (i < strlen) {
      final char signCh = str.charAt(i);
      if (signCh == '-' || signCh == '+') {
        i++;
        if (signCh == '-') {
          isNegative = true;
        }
      }
    }

    // scan all digits and accumulate into an integral value
    // Keep track of the location of the decimal point (if any) to allow scaling
    // later
    final DD val = new DD();

    int numDigits = 0;
    int numBeforeDec = 0;
    int exp = 0;
    while (true) {
      if (i >= strlen) {
        break;
      }
      final char ch = str.charAt(i);
      i++;
      if (Character.isDigit(ch)) {
        final double d = ch - '0';
        val.selfMultiply(TEN);
        // MD: need to optimize this
        val.selfAdd(d);
        numDigits++;
        continue;
      }
      if (ch == '.') {
        numBeforeDec = numDigits;
        continue;
      }
      if (ch == 'e' || ch == 'E') {
        final String expStr = str.substring(i);
        // this should catch any format problems with the exponent
        try {
          exp = Integer.parseInt(expStr);
        } catch (final NumberFormatException ex) {
          throw new NumberFormatException("Invalid exponent " + expStr + " in string " + str);
        }
        break;
      }
      throw new NumberFormatException(
        "Unexpected character '" + ch + "' at position " + i + " in string " + str);
    }
    DD val2 = val;

    // scale the number correctly
    final int numDecPlaces = numDigits - numBeforeDec - exp;
    if (numDecPlaces == 0) {
      val2 = val;
    } else if (numDecPlaces > 0) {
      final DD scale = TEN.pow(numDecPlaces);
      val2 = val.divide(scale);
    } else if (numDecPlaces < 0) {
      final DD scale = TEN.pow(-numDecPlaces);
      val2 = val.multiply(scale);
    }
    // apply leading sign, if any
    if (isNegative) {
      return val2.negate();
    }
    return val2;

  }

  /**
   * Computes the square of this value.
   *
   * @return the square of this value.
   */
  public static DD sqr(final double x) {
    return valueOf(x).selfMultiply(x);
  }

  public static DD sqrt(final double x) {
    return valueOf(x).sqrt();
  }

  /**
   * Creates a string of a given length containing the given character
   *
   * @param ch the character to be repeated
   * @param len the len of the desired string
   * @return the string
   */
  private static String stringOfChar(final char ch, final int len) {
    final StringBuilder buf = new StringBuilder();
    for (int i = 0; i < len; i++) {
      buf.append(ch);
    }
    return buf.toString();
  }

  /**
   * Converts the <tt>double</tt> argument to a DoubleDouble number.
   *
   * @param x a numeric value
   * @return the extended precision version of the value
   */
  public static DD valueOf(final double x) {
    return new DD(x);
  }

  /**
   * Converts the string argument to a DoubleDouble number.
   *
   * @param str a string containing a representation of a numeric value
   * @return the extended precision version of the value
   * @throws NumberFormatException if <tt>s</tt> is not a valid representation of a number
   */
  public static DD valueOf(final String str) throws NumberFormatException {
    return parse(str);
  }

  /**
   * The high-order component of the double-double precision value.
   */
  private double hi = 0.0;

  /*
   * double getHighComponent() { return hi; } double getLowComponent() { return
   * lo; }
   */

  // Testing only - should not be public
  /*
   * public void RENORM() { double s = hi + lo; double err = lo - (s - hi); hi =
   * s; lo = err; }
   */

  /**
   * The low-order component of the double-double precision value.
   */
  private double lo = 0.0;

  /**
   * Creates a new DoubleDouble with value 0.0.
   */
  public DD() {
    init(0.0);
  }

  /**
   * Creates a new DoubleDouble with value equal to the argument.
   *
   * @param dd the value to initialize
   */
  public DD(final DD dd) {
    init(dd);
  }

  /**
   * Creates a new DoubleDouble with value x.
   *
   * @param x the value to initialize
   */
  public DD(final double x) {
    init(x);
  }

  /**
   * Creates a new DoubleDouble with value (hi, lo).
   *
   * @param hi the high-order component
   * @param lo the high-order component
   */
  public DD(final double hi, final double lo) {
    init(hi, lo);
  }

  /**
   * Creates a new DoubleDouble with value equal to the argument.
   *
   * @param str the value to initialize by
   * @throws NumberFormatException if <tt>str</tt> is not a valid representation of a number
   */
  public DD(final String str) throws NumberFormatException {
    this(parse(str));
  }

  /**
   * Returns the absolute value of this value.
   * Special cases:
   * <ul>
   * <li>If this value is NaN, it is returned.
   * </ul>
   *
   * @return the absolute value of this value
   */
  public DD abs() {
    if (isNaN()) {
      return NaN;
    }
    if (isNegative()) {
      return negate();
    }
    return new DD(this);
  }

  /**
   * Returns a DoubleDouble whose value is <tt>(this + y)</tt>.
   *
   * @param y the addend
   * @return <tt>(this + y)</tt>
   */
  public final DD add(final DD y) {
    return copy(this).selfAdd(y);
  }

  /**
   * Returns a DoubleDouble whose value is <tt>(this + y)</tt>.
   *
   * @param y the addend
   * @return <tt>(this + y)</tt>
   */
  public final DD add(final double y) {
    return copy(this).selfAdd(y);
  }

  /**
   * Returns the smallest (closest to negative infinity) value
   * that is not less than the argument and is equal to a mathematical integer.
   * Special cases:
   * <ul>
   * <li>If this value is NaN, returns NaN.
   * </ul>
   *
   * @return the smallest (closest to negative infinity) value
   * that is not less than the argument and is equal to a mathematical integer.
   */
  public DD ceil() {
    if (isNaN()) {
      return NaN;
    }
    final double fhi = Math.ceil(this.hi);
    double flo = 0.0;
    // Hi is already integral. Ceil the low word
    if (fhi == this.hi) {
      flo = Math.ceil(this.lo);
      // do we need to renormalize here?
    }
    return new DD(fhi, flo);
  }

  /**
   * Creates and returns a copy of this value.
   *
   * @return a copy of this value
   */
  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (final CloneNotSupportedException ex) {
      // should never reach here
      return null;
    }
  }

  /**
   * Compares two DoubleDouble objects numerically.
   *
   * @return -1,0 or 1 depending on whether this value is less than, equal to
   * or greater than the value of <tt>o</tt>
   */
  @Override
  public int compareTo(final Object o) {
    final DD other = (DD)o;

    if (this.hi < other.hi) {
      return -1;
    }
    if (this.hi > other.hi) {
      return 1;
    }
    if (this.lo < other.lo) {
      return -1;
    }
    if (this.lo > other.lo) {
      return 1;
    }
    return 0;
  }

  /**
   * Computes a new DoubleDouble whose value is <tt>(this / y)</tt>.
   *
   * @param y the divisor
   * @return a new object with the value <tt>(this / y)</tt>
   */
  public final DD divide(final DD y) {
    double hc, tc, hy, ty, C, c, U, u;
    C = this.hi / y.hi;
    c = SPLIT * C;
    hc = c - C;
    u = SPLIT * y.hi;
    hc = c - hc;
    tc = C - hc;
    hy = u - y.hi;
    U = C * y.hi;
    hy = u - hy;
    ty = y.hi - hy;
    u = hc * hy - U + hc * ty + tc * hy + tc * ty;
    c = (this.hi - U - u + this.lo - C * y.lo) / y.hi;
    u = C + c;

    final double zhi = u;
    final double zlo = C - u + c;
    return new DD(zhi, zlo);
  }

  /**
   * Computes a new DoubleDouble whose value is <tt>(this / y)</tt>.
   *
   * @param y the divisor
   * @return a new object with the value <tt>(this / y)</tt>
   */
  public final DD divide(final double y) {
    if (Double.isNaN(y)) {
      return newNaN();
    }
    return copy(this).selfDivide(y, 0.0);
  }

  /**
   * Converts this value to the nearest double-precision number.
   *
   * @return the nearest double-precision number to this value
   */
  public double doubleValue() {
    return this.hi + this.lo;
  }

  /**
   * Dumps the components of this number to a string.
   *
   * @return a string showing the components of the number
   */
  public String dump() {
    return "DD<" + this.hi + ", " + this.lo + ">";
  }

  /**
   * Tests whether this value is equal to another <tt>DoubleDouble</tt> value.
   *
   * @param y a DoubleDouble value
   * @return true if this value = y
   */
  public boolean equals(final DD y) {
    return this.hi == y.hi && this.lo == y.lo;
  }

  /**
   * Extracts the significant digits in the decimal representation of the argument.
   * A decimal point may be optionally inserted in the string of digits
   * (as long as its position lies within the extracted digits
   * - if not, the caller must prepend or append the appropriate zeroes and decimal point).
   *
   * @param y the number to extract ( >= 0)
   * @param decimalPointPos the position in which to insert a decimal point
   * @return the string containing the significant digits and possibly a decimal point
   */
  private String extractSignificantDigits(final boolean insertDecimalPoint, final int[] magnitude) {
    DD y = this.abs();
    // compute *correct* magnitude of y
    int mag = magnitude(y.hi);
    final DD scale = TEN.pow(mag);
    y = y.divide(scale);

    // fix magnitude if off by one
    if (y.gt(TEN)) {
      y = y.divide(TEN);
      mag += 1;
    } else if (y.lt(ONE)) {
      y = y.multiply(TEN);
      mag -= 1;
    }

    final int decimalPointPos = mag + 1;
    final StringBuilder buf = new StringBuilder();
    final int numDigits = MAX_PRINT_DIGITS - 1;
    for (int i = 0; i <= numDigits; i++) {
      if (insertDecimalPoint && i == decimalPointPos) {
        buf.append('.');
      }
      final int digit = (int)y.hi;
      // System.out.println("printDump: [" + i + "] digit: " + digit + " y: " +
      // y.dump() + " buf: " + buf);

      /**
       * This should never happen, due to heuristic checks on remainder below
       */
      if (digit < 0 || digit > 9) {
        // System.out.println("digit > 10 : " + digit);
        // throw new IllegalStateException("Internal errror: found digit = " +
        // digit);
      }
      /**
       * If a negative remainder is encountered, simply terminate the extraction.
       * This is robust, but maybe slightly inaccurate.
       * My current hypothesis is that negative remainders only occur for very small lo components,
       * so the inaccuracy is tolerable
       */
      if (digit < 0) {
        break;
        // throw new IllegalStateException("Internal errror: found digit = " +
        // digit);
      }
      boolean rebiasBy10 = false;
      char digitChar = 0;
      if (digit > 9) {
        // set flag to re-bias after next 10-shift
        rebiasBy10 = true;
        // output digit will end up being '9'
        digitChar = '9';
      } else {
        digitChar = (char)('0' + digit);
      }
      buf.append(digitChar);
      y = y.subtract(DD.valueOf(digit)).multiply(TEN);
      if (rebiasBy10) {
        y.selfAdd(TEN);
      }

      boolean continueExtractingDigits = true;
      /**
       * Heuristic check: if the remaining portion of
       * y is non-positive, assume that output is complete
       */
      // if (y.hi <= 0.0)
      // if (y.hi < 0.0)
      // continueExtractingDigits = false;
      /**
       * Check if remaining digits will be 0, and if so don't output them.
       * Do this by comparing the magnitude of the remainder with the expected precision.
       */
      final int remMag = magnitude(y.hi);
      if (remMag < 0 && Math.abs(remMag) >= numDigits - i) {
        continueExtractingDigits = false;
      }
      if (!continueExtractingDigits) {
        break;
      }
    }
    magnitude[0] = mag;
    return buf.toString();
  }

  /**
   * Returns the largest (closest to positive infinity)
   * value that is not greater than the argument
   * and is equal to a mathematical integer.
   * Special cases:
   * <ul>
   * <li>If this value is NaN, returns NaN.
   * </ul>
   *
   * @return the largest (closest to positive infinity)
   * value that is not greater than the argument
   * and is equal to a mathematical integer.
   */
  public DD floor() {
    if (isNaN()) {
      return NaN;
    }
    final double fhi = Math.floor(this.hi);
    double flo = 0.0;
    // Hi is already integral. Floor the low word
    if (fhi == this.hi) {
      flo = Math.floor(this.lo);
    }
    // do we need to renormalize here?
    return new DD(fhi, flo);
  }

  /**
   * Tests whether this value is greater than or equals to another <tt>DoubleDouble</tt> value.
   * @param y a DoubleDouble value
   * @return true if this value >= y
   */
  public boolean ge(final DD y) {
    return this.hi > y.hi || this.hi == y.hi && this.lo >= y.lo;
  }

  /**
   * Returns the string for this value if it has a known representation.
   * (E.g. NaN or 0.0)
   *
   * @return the string for this special number
   * or null if the number is not a special number
   */
  private String getSpecialNumberString() {
    if (isZero()) {
      return "0.0";
    }
    if (isNaN()) {
      return "NaN ";
    }
    return null;
  }

  /**
   * Tests whether this value is greater than another <tt>DoubleDouble</tt> value.
   * @param y a DoubleDouble value
   * @return true if this value > y
   */
  public boolean gt(final DD y) {
    return this.hi > y.hi || this.hi == y.hi && this.lo > y.lo;
  }

  private final void init(final DD dd) {
    this.hi = dd.hi;
    this.lo = dd.lo;
  }

  private final void init(final double x) {
    this.hi = x;
    this.lo = 0.0;
  }

  private final void init(final double hi, final double lo) {
    this.hi = hi;
    this.lo = lo;
  }

  /**
   * Converts this value to the nearest integer.
   *
   * @return the nearest integer to this value
   */
  public int intValue() {
    return (int)this.hi;
  }

  /**
   * Tests whether this value is NaN.
   *
   * @return true if this value is NaN
   */
  public boolean isNaN() {
    return Double.isNaN(this.hi);
  }

  /**
   * Tests whether this value is less than 0.
   *
   * @return true if this value is less than 0
   */
  public boolean isNegative() {
    return this.hi < 0.0 || this.hi == 0.0 && this.lo < 0.0;
  }

  /**
   * Tests whether this value is greater than 0.
   *
   * @return true if this value is greater than 0
   */
  public boolean isPositive() {
    return this.hi > 0.0 || this.hi == 0.0 && this.lo > 0.0;
  }

  /**
   * Tests whether this value is equal to 0.
   *
   * @return true if this value is equal to 0
   */
  public boolean isZero() {
    return this.hi == 0.0 && this.lo == 0.0;
  }

  /**
   * Tests whether this value is less than or equal to another <tt>DoubleDouble</tt> value.
   * @param y a DoubleDouble value
   * @return true if this value <= y
   */
  public boolean le(final DD y) {
    return this.hi < y.hi || this.hi == y.hi && this.lo <= y.lo;
  }

  /**
   * Tests whether this value is less than another <tt>DoubleDouble</tt> value.
   * @param y a DoubleDouble value
   * @return true if this value < y
   */
  public boolean lt(final DD y) {
    return this.hi < y.hi || this.hi == y.hi && this.lo < y.lo;
  }

  /*------------------------------------------------------------
   *   Ordering Functions
   *------------------------------------------------------------
   */

  /**
   * Computes the maximum of this and another DD number.
   *
   * @param x a DD number
   * @return the maximum of the two numbers
   */
  public DD max(final DD x) {
    if (this.ge(x)) {
      return this;
    } else {
      return x;
    }
  }

  /**
   * Computes the minimum of this and another DD number.
   *
   * @param x a DD number
   * @return the minimum of the two numbers
   */
  public DD min(final DD x) {
    if (this.le(x)) {
      return this;
    } else {
      return x;
    }
  }

  /*------------------------------------------------------------
   *   Conversion Functions
   *------------------------------------------------------------
   */

  /**
   * Returns a new DoubleDouble whose value is <tt>(this * y)</tt>.
   *
   * @param y the multiplicand
   * @return <tt>(this * y)</tt>
   */
  public final DD multiply(final DD y) {
    if (y.isNaN()) {
      return newNaN();
    }
    return copy(this).selfMultiply(y);
  }

  /**
   * Returns a new DoubleDouble whose value is <tt>(this * y)</tt>.
   *
   * @param y the multiplicand
   * @return <tt>(this * y)</tt>
   */
  public final DD multiply(final double y) {
    if (Double.isNaN(y)) {
      return newNaN();
    }
    return copy(this).selfMultiply(y, 0.0);
  }

  /*------------------------------------------------------------
   *   Predicates
   *------------------------------------------------------------
   */

  /**
   * Returns a DoubleDouble whose value is <tt>-this</tt>.
   *
   * @return <tt>-this</tt>
   */
  public final DD negate() {
    if (isNaN()) {
      return this;
    }
    return new DD(-this.hi, -this.lo);
  }

  /**
   * Computes the value of this number raised to an integral power.
   * Follows semantics of Java Math.pow as closely as possible.
   *
   * @param exp the integer exponent
   * @return x raised to the integral power exp
   */
  public DD pow(final int exp) {
    if (exp == 0.0) {
      return valueOf(1.0);
    }

    DD r = new DD(this);
    DD s = valueOf(1.0);
    int n = Math.abs(exp);

    if (n > 1) {
      /* Use binary exponentiation */
      while (n > 0) {
        if (n % 2 == 1) {
          s.selfMultiply(r);
        }
        n /= 2;
        if (n > 0) {
          r = r.sqr();
        }
      }
    } else {
      s = r;
    }

    /* Compute the reciprocal if n is negative. */
    if (exp < 0) {
      return s.reciprocal();
    }
    return s;
  }

  /**
   * Returns a DoubleDouble whose value is  <tt>1 / this</tt>.
   *
   * @return the reciprocal of this value
   */
  public final DD reciprocal() {
    double hc, tc, hy, ty, C, c, U, u;
    C = 1.0 / this.hi;
    c = SPLIT * C;
    hc = c - C;
    u = SPLIT * this.hi;
    hc = c - hc;
    tc = C - hc;
    hy = u - this.hi;
    U = C * this.hi;
    hy = u - hy;
    ty = this.hi - hy;
    u = hc * hy - U + hc * ty + tc * hy + tc * ty;
    c = (1.0 - U - u - C * this.lo) / this.hi;

    final double zhi = C + c;
    final double zlo = C - zhi + c;
    return new DD(zhi, zlo);
  }

  /**
   * Rounds this value to the nearest integer.
   * The value is rounded to an integer by adding 1/2 and taking the floor of the result.
   * Special cases:
   * <ul>
   * <li>If this value is NaN, returns NaN.
   * </ul>
   *
   * @return this value rounded to the nearest integer
   */
  public DD rint() {
    if (isNaN()) {
      return this;
    }
    // may not be 100% correct
    final DD plus5 = this.add(0.5);
    return plus5.floor();
  }

  /**
   * Adds the argument to the value of <tt>this</tt>.
   * To prevent altering constants,
   * this method <b>must only</b> be used on values known to
   * be newly created.
   *
   * @param y the addend
   * @return this object, increased by y
   */
  public final DD selfAdd(final DD y) {
    return selfAdd(y.hi, y.lo);
  }

  /**
   * Adds the argument to the value of <tt>this</tt>.
   * To prevent altering constants,
   * this method <b>must only</b> be used on values known to
   * be newly created.
   *
   * @param y the addend
   * @return this object, increased by y
   */
  public final DD selfAdd(final double y) {
    double H, h, S, s, e, f;
    S = this.hi + y;
    e = S - this.hi;
    s = S - e;
    s = y - e + (this.hi - s);
    f = s + this.lo;
    H = S + f;
    h = f + (S - H);
    this.hi = H + h;
    this.lo = h + (H - this.hi);
    return this;
    // return selfAdd(y, 0.0);
  }

  private final DD selfAdd(final double yhi, final double ylo) {
    double H, h, T, t, S, s, e, f;
    S = this.hi + yhi;
    T = this.lo + ylo;
    e = S - this.hi;
    f = T - this.lo;
    s = S - e;
    t = T - f;
    s = yhi - e + (this.hi - s);
    t = ylo - f + (this.lo - t);
    e = s + T;
    H = S + e;
    h = e + (S - H);
    e = t + h;

    final double zhi = H + e;
    final double zlo = e + (H - zhi);
    this.hi = zhi;
    this.lo = zlo;
    return this;
  }

  /**
   * Divides this object by the argument, returning <tt>this</tt>.
   * To prevent altering constants,
   * this method <b>must only</b> be used on values known to
   * be newly created.
   *
   * @param y the value to divide by
   * @return this object, divided by y
   */
  public final DD selfDivide(final DD y) {
    return selfDivide(y.hi, y.lo);
  }

  /**
   * Divides this object by the argument, returning <tt>this</tt>.
   * To prevent altering constants,
   * this method <b>must only</b> be used on values known to
   * be newly created.
   *
   * @param y the value to divide by
   * @return this object, divided by y
   */
  public final DD selfDivide(final double y) {
    return selfDivide(y, 0.0);
  }

  private final DD selfDivide(final double yhi, final double ylo) {
    double hc, tc, hy, ty, C, c, U, u;
    C = this.hi / yhi;
    c = SPLIT * C;
    hc = c - C;
    u = SPLIT * yhi;
    hc = c - hc;
    tc = C - hc;
    hy = u - yhi;
    U = C * yhi;
    hy = u - hy;
    ty = yhi - hy;
    u = hc * hy - U + hc * ty + tc * hy + tc * ty;
    c = (this.hi - U - u + this.lo - C * ylo) / yhi;
    u = C + c;

    this.hi = u;
    this.lo = C - u + c;
    return this;
  }

  /*------------------------------------------------------------
   *   Output
   *------------------------------------------------------------
   */

  /**
   * Multiplies this object by the argument, returning <tt>this</tt>.
   * To prevent altering constants,
   * this method <b>must only</b> be used on values known to
   * be newly created.
   *
   * @param y the value to multiply by
   * @return this object, multiplied by y
   */
  public final DD selfMultiply(final DD y) {
    return selfMultiply(y.hi, y.lo);
  }

  /**
   * Multiplies this object by the argument, returning <tt>this</tt>.
   * To prevent altering constants,
   * this method <b>must only</b> be used on values known to
   * be newly created.
   *
   * @param y the value to multiply by
   * @return this object, multiplied by y
   */
  public final DD selfMultiply(final double y) {
    return selfMultiply(y, 0.0);
  }

  private final DD selfMultiply(final double yhi, final double ylo) {
    double hx, tx, hy, ty, C, c;
    C = SPLIT * this.hi;
    hx = C - this.hi;
    c = SPLIT * yhi;
    hx = C - hx;
    tx = this.hi - hx;
    hy = c - yhi;
    C = this.hi * yhi;
    hy = c - hy;
    ty = yhi - hy;
    c = hx * hy - C + hx * ty + tx * hy + tx * ty + (this.hi * ylo + this.lo * yhi);
    final double zhi = C + c;
    hx = C - zhi;
    final double zlo = c + hx;
    this.hi = zhi;
    this.lo = zlo;
    return this;
  }

  /**
   * Subtracts the argument from the value of <tt>this</tt>.
   * To prevent altering constants,
   * this method <b>must only</b> be used on values known to
   * be newly created.
   *
   * @param y the addend
   * @return this object, decreased by y
   */
  public final DD selfSubtract(final DD y) {
    if (isNaN()) {
      return this;
    }
    return selfAdd(-y.hi, -y.lo);
  }

  /**
   * Subtracts the argument from the value of <tt>this</tt>.
   * To prevent altering constants,
   * this method <b>must only</b> be used on values known to
   * be newly created.
   *
   * @param y the addend
   * @return this object, decreased by y
   */
  public final DD selfSubtract(final double y) {
    if (isNaN()) {
      return this;
    }
    return selfAdd(-y, 0.0);
  }

  /**
   * Returns an integer indicating the sign of this value.
   * <ul>
   * <li>if this value is > 0, returns 1
   * <li>if this value is < 0, returns -1
   * <li>if this value is = 0, returns 0
   * <li>if this value is NaN, returns 0
   * </ul>
   *
   * @return an integer indicating the sign of this value
   */
  public int signum() {
    if (this.hi > 0) {
      return 1;
    }
    if (this.hi < 0) {
      return -1;
    }
    if (this.lo > 0) {
      return 1;
    }
    if (this.lo < 0) {
      return -1;
    }
    return 0;
  }

  /**
   * Computes the square of this value.
   *
   * @return the square of this value.
   */
  public DD sqr() {
    return this.multiply(this);
  }

  /**
   * Computes the positive square root of this value.
   * If the number is NaN or negative, NaN is returned.
   *
   * @return the positive square root of this number.
   * If the argument is NaN or less than zero, the result is NaN.
   */
  public DD sqrt() {
    /*
     * Strategy: Use Karp's trick: if x is an approximation to sqrt(a), then
     * sqrt(a) = a*x + [a - (a*x)^2] * x / 2 (approx) The approximation is
     * accurate to twice the accuracy of x. Also, the multiplication (a*x) and
     * [-]*x can be done with only half the precision.
     */

    if (isZero()) {
      return valueOf(0.0);
    }

    if (isNegative()) {
      return NaN;
    }

    final double x = 1.0 / Math.sqrt(this.hi);
    final double ax = this.hi * x;

    final DD axdd = valueOf(ax);
    final DD diffSq = this.subtract(axdd.sqr());
    final double d2 = diffSq.hi * (x * 0.5);

    return axdd.add(d2);
  }

  /**
   * Computes a new DoubleDouble object whose value is <tt>(this - y)</tt>.
   *
   * @param y the subtrahend
   * @return <tt>(this - y)</tt>
   */
  public final DD subtract(final DD y) {
    return add(y.negate());
  }

  /**
   * Computes a new DoubleDouble object whose value is <tt>(this - y)</tt>.
   *
   * @param y the subtrahend
   * @return <tt>(this - y)</tt>
   */
  public final DD subtract(final double y) {
    return add(-y);
  }

  /**
   * Returns the string representation of this value in scientific notation.
   *
   * @return the string representation in scientific notation
   */
  public String toSciNotation() {
    // special case zero, to allow as
    if (isZero()) {
      return SCI_NOT_ZERO;
    }

    final String specialStr = getSpecialNumberString();
    if (specialStr != null) {
      return specialStr;
    }

    final int[] magnitude = new int[1];
    final String digits = extractSignificantDigits(false, magnitude);
    final String expStr = SCI_NOT_EXPONENT_CHAR + magnitude[0];

    // should never have leading zeroes
    // MD - is this correct? Or should we simply strip them if they are present?
    if (digits.charAt(0) == '0') {
      throw new IllegalStateException("Found leading zero: " + digits);
    }

    // add decimal point
    String trailingDigits = "";
    if (digits.length() > 1) {
      trailingDigits = digits.substring(1);
    }
    final String digitsWithDecimal = digits.charAt(0) + "." + trailingDigits;

    if (this.isNegative()) {
      return "-" + digitsWithDecimal + expStr;
    }
    return digitsWithDecimal + expStr;
  }

  /**
   * Returns the string representation of this value in standard notation.
   *
   * @return the string representation in standard notation
   */
  public String toStandardNotation() {
    final String specialStr = getSpecialNumberString();
    if (specialStr != null) {
      return specialStr;
    }

    final int[] magnitude = new int[1];
    final String sigDigits = extractSignificantDigits(true, magnitude);
    final int decimalPointPos = magnitude[0] + 1;

    String num = sigDigits;
    // add a leading 0 if the decimal point is the first char
    if (sigDigits.charAt(0) == '.') {
      num = "0" + sigDigits;
    } else if (decimalPointPos < 0) {
      num = "0." + stringOfChar('0', -decimalPointPos) + sigDigits;
    } else if (sigDigits.indexOf('.') == -1) {
      // no point inserted - sig digits must be smaller than magnitude of number
      // add zeroes to end to make number the correct size
      final int numZeroes = decimalPointPos - sigDigits.length();
      final String zeroes = stringOfChar('0', numZeroes);
      num = sigDigits + zeroes + ".0";
    }

    if (this.isNegative()) {
      return "-" + num;
    }
    return num;
  }

  /**
   * Returns a string representation of this number, in either standard or scientific notation.
   * If the magnitude of the number is in the range [ 10<sup>-3</sup>, 10<sup>8</sup> ]
   * standard notation will be used.  Otherwise, scientific notation will be used.
   *
   * @return a string representation of this number
   */
  @Override
  public String toString() {
    final int mag = magnitude(this.hi);
    if (mag >= -3 && mag <= 20) {
      return toStandardNotation();
    }
    return toSciNotation();
  }

  /*------------------------------------------------------------
   *   Input
   *------------------------------------------------------------
   */

  /**
   * Returns the integer which is largest in absolute value and not further
   * from zero than this value.
   * Special cases:
   * <ul>
   * <li>If this value is NaN, returns NaN.
   * </ul>
   *
   * @return the integer which is largest in absolute value and not further from zero than this value
   */
  public DD trunc() {
    if (isNaN()) {
      return NaN;
    }
    if (isPositive()) {
      return floor();
    } else {
      return ceil();
    }
  }
}
