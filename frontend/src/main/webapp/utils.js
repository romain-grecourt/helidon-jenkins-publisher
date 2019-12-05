export default {
    getParent: function (comp, name) {
        let p = comp.$parent;
        while (typeof p !== 'undefined') {
            if (p.$options.name === name) {
                return p;
            } else {
                p = p.$parent;
            }
        }
        return false;
    }
}
