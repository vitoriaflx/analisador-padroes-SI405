interface A {}

class B implements A {}

class C {
    public A m() {
        return new B();
    }
}
