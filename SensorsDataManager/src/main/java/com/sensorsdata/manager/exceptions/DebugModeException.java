/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2020 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.manager.exceptions;

/**
 * Debug 模式下的异常，用于指出 Debug 模式下的各种问题，程序不应该捕获该异常，以免屏蔽错误信息
 */
public class DebugModeException extends RuntimeException {

    public DebugModeException(String error) {
        super(error);
    }

    public DebugModeException(Throwable throwable) {
        super(throwable);
    }

}
