package com.laodu.p367;

/**
 * @Description
 * @Author zhou
 * @Date 2026/10/30
 */

public class MyLink<E> {

    private int size;
    private Node<E> head;

    private static class Node<E> {
        @Override
        public String toString() {
            return "Node{" +
                    "data=" + data +
                    ", next=" + next +
                    '}';
        }

        private E data;
        private Node<E> next;

        public Node(E data, Node<E> next) {
            this.data = data;
            this.next = next;
        }


    }

    public void add(E data) {
        Node<E> node = new Node<>(data, null);
        if (this.head == null) {
            head = node;
        } else {
            Node<E> lastNode = findLast();
            lastNode.next = node;

        }
        size++;
    }

    public void add(int index, E data) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index: " + index + ", size: " + size);
        }
        Node<E> node = new Node<>(data, null);
        if (index == 0) {
            node.next = head;
            head = node;
        } else {
            Node<E> preNode = getNode(index - 1);
            node.next = preNode.next;
            preNode.next = node;
        }
        size++;
    }

    public void remove(int index) {
        checkIndexBounds(index);
        if (index == 0) {
            Node<E> nextNode = head.next;
            head.next = null;
            head = nextNode;

        } else {
            Node<E> preNode = getNode(index - 1);
            Node<E> curNode = preNode.next;
            preNode.next = preNode.next.next;
            curNode.next = null;
        }
        size--;
    }


    public boolean remove(E data) {
        if (head == null || data == null) {
            return false;
        }
        Node<E> node = null;
        Node<E> preNode = null;
        for (int i = 0; i < size; i++) {
            if (i == 0) {
                node = head;
                if (data.equals(node.data)) {
                    head = node.next;
                    node.next = null;
                    size--;
                    return true;
                }
            } else {
                if (data.equals(node.data)) {
                    preNode.next = node.next;
                    node.next = null;
                    size--;
                    return true;
                }

            }
            preNode = node;
            node = node.next;
        }
        return false;
    }

    public boolean set(int index, E data) {
        checkIndexBounds(index);
        Node<E> node = getNode(index);
        node.data = data;
        return true;
    }


    public E get(int index) {
        checkIndexBounds(index);
        return getNode(index).data;
    }

    private Node<E> getNode(int index) {

        Node<E> current = head;
        for (int i = 1; i < index + 1; i++) {
            current = current.next;
        }
        return current;

    }

    private void checkIndexBounds(int index) {
        if (index < 0 || index > size - 1) {
            throw new IndexOutOfBoundsException();
        }
    }

    private Node<E> findLast() {
        return getNode(size - 1);
    }


}
