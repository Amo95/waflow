package com.infusi.waflow.menu;

/**
 * A single item in a menu, mapping to a flow.
 *
 * @param id         unique identifier (used as list row / button id)
 * @param label      display label
 * @param flowId     the flow to start when selected
 */
public record MenuItem(String id, String label, String flowId) {}
