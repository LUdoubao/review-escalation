/**
 * 防抖工具：用于关键字检索等高频输入场景，避免每次按键都发起请求。
 */

/**
 * 生成一个防抖函数：在最后一次调用后的等待期内无新调用时才真正执行。
 *
 * @param {Function} fn 需要防抖的函数
 * @param {number} [wait] 等待毫秒数，默认 250
 * @returns {Function} 防抖后的函数，保留原调用的 this 与参数
 */
export function debounce(fn, wait = 250) {
  let timer = null
  return function debounced(...args) {
    if (timer) {
      clearTimeout(timer)
    }
    timer = setTimeout(() => {
      timer = null
      fn.apply(this, args)
    }, wait)
  }
}
