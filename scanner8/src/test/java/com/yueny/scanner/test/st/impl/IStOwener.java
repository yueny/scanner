package com.yueny.scanner.test.st.impl;

import com.yueny.scanner.test.st.ISt;
import com.yueny.scanner.test.st.anno.St;

/**
 * @Author yueny09 <deep_blue_yang@126.com>
 * @Date 2019-09-04 22:49
 */
public interface IStOwener extends ISt {

    @St
    interface ITTOwener extends ISt {
        //.
    }
}
