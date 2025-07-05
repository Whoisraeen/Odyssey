package com.odyssey.ui.layout;

import com.odyssey.ui.UIComponent;

import java.util.*;

/**
 * Relative layout manager that positions children relative to each other or the parent
 * Supports complex positioning rules and automatic dependency resolution
 */
public class RelativeLayout extends LayoutManager {
    
    public static class RelativeLayoutParams extends LayoutParams {
        // Alignment relative to parent
        public boolean alignParentLeft = false;
        public boolean alignParentTop = false;
        public boolean alignParentRight = false;
        public boolean alignParentBottom = false;
        public boolean centerInParent = false;
        public boolean centerHorizontal = false;
        public boolean centerVertical = false;
        
        // Alignment relative to other views
        public UIComponent leftOf = null;
        public UIComponent rightOf = null;
        public UIComponent above = null;
        public UIComponent below = null;
        public UIComponent alignLeft = null;
        public UIComponent alignTop = null;
        public UIComponent alignRight = null;
        public UIComponent alignBottom = null;
        public UIComponent alignBaseline = null;
        
        public RelativeLayoutParams() {
            super();
        }
        
        public RelativeLayoutParams(int width, int height) {
            super(width, height);
        }
        
        // Parent alignment methods
        public RelativeLayoutParams alignParentLeft() {
            this.alignParentLeft = true;
            return this;
        }
        
        public RelativeLayoutParams alignParentTop() {
            this.alignParentTop = true;
            return this;
        }
        
        public RelativeLayoutParams alignParentRight() {
            this.alignParentRight = true;
            return this;
        }
        
        public RelativeLayoutParams alignParentBottom() {
            this.alignParentBottom = true;
            return this;
        }
        
        public RelativeLayoutParams centerInParent() {
            this.centerInParent = true;
            return this;
        }
        
        public RelativeLayoutParams centerHorizontal() {
            this.centerHorizontal = true;
            return this;
        }
        
        public RelativeLayoutParams centerVertical() {
            this.centerVertical = true;
            return this;
        }
        
        // Relative positioning methods
        public RelativeLayoutParams leftOf(UIComponent component) {
            this.leftOf = component;
            return this;
        }
        
        public RelativeLayoutParams rightOf(UIComponent component) {
            this.rightOf = component;
            return this;
        }
        
        public RelativeLayoutParams above(UIComponent component) {
            this.above = component;
            return this;
        }
        
        public RelativeLayoutParams below(UIComponent component) {
            this.below = component;
            return this;
        }
        
        public RelativeLayoutParams alignLeft(UIComponent component) {
            this.alignLeft = component;
            return this;
        }
        
        public RelativeLayoutParams alignTop(UIComponent component) {
            this.alignTop = component;
            return this;
        }
        
        public RelativeLayoutParams alignRight(UIComponent component) {
            this.alignRight = component;
            return this;
        }
        
        public RelativeLayoutParams alignBottom(UIComponent component) {
            this.alignBottom = component;
            return this;
        }
        
        public RelativeLayoutParams alignBaseline(UIComponent component) {
            this.alignBaseline = component;
            return this;
        }
        
        /**
         * Get all components this layout params depends on
         */
        public Set<UIComponent> getDependencies() {
            Set<UIComponent> dependencies = new HashSet<>();
            if (leftOf != null) dependencies.add(leftOf);
            if (rightOf != null) dependencies.add(rightOf);
            if (above != null) dependencies.add(above);
            if (below != null) dependencies.add(below);
            if (alignLeft != null) dependencies.add(alignLeft);
            if (alignTop != null) dependencies.add(alignTop);
            if (alignRight != null) dependencies.add(alignRight);
            if (alignBottom != null) dependencies.add(alignBottom);
            if (alignBaseline != null) dependencies.add(alignBaseline);
            return dependencies;
        }
    }
    
    private static class LayoutNode {
        UIComponent component;
        RelativeLayoutParams params;
        boolean measured = false;
        boolean positioned = false;
        
        LayoutNode(UIComponent component, RelativeLayoutParams params) {
            this.component = component;
            this.params = params;
        }
    }
    
    public RelativeLayout(UIComponent parent) {
        super(parent);
    }
    
    @Override
    public void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
        List<UIComponent> children = getVisibleChildren();
        if (children.isEmpty()) {
            parent.setMeasuredDimension(
                resolveSize(parent.getPaddingLeft() + parent.getPaddingRight(), widthMeasureSpec),
                resolveSize(parent.getPaddingTop() + parent.getPaddingBottom(), heightMeasureSpec)
            );
            return;
        }
        
        // Create layout nodes
        List<LayoutNode> nodes = new ArrayList<>();
        for (UIComponent child : children) {
            LayoutParams lp = child.getLayoutParams();
            RelativeLayoutParams rlp = (lp instanceof RelativeLayoutParams) ? 
                (RelativeLayoutParams) lp : new RelativeLayoutParams();
            nodes.add(new LayoutNode(child, rlp));
        }
        
        // Sort nodes by dependency order
        List<LayoutNode> sortedNodes = topologicalSort(nodes);
        
        // Measure children in dependency order
        for (LayoutNode node : sortedNodes) {
            measureChild(node.component, widthMeasureSpec, heightMeasureSpec);
            node.measured = true;
        }
        
        // Calculate parent dimensions
        int maxWidth = 0;
        int maxHeight = 0;
        
        for (LayoutNode node : nodes) {
            UIComponent child = node.component;
            RelativeLayoutParams rlp = node.params;
            
            int childRight = child.getMeasuredWidth() + rlp.getTotalMarginHorizontal();
            int childBottom = child.getMeasuredHeight() + rlp.getTotalMarginVertical();
            
            maxWidth = Math.max(maxWidth, childRight);
            maxHeight = Math.max(maxHeight, childBottom);
        }
        
        maxWidth += parent.getPaddingLeft() + parent.getPaddingRight();
        maxHeight += parent.getPaddingTop() + parent.getPaddingBottom();
        
        parent.setMeasuredDimension(
            resolveSize(maxWidth, widthMeasureSpec),
            resolveSize(maxHeight, heightMeasureSpec)
        );
    }
    
    @Override
    public void layoutChildren(int left, int top, int right, int bottom) {
        List<UIComponent> children = getVisibleChildren();
        if (children.isEmpty()) return;
        
        int paddingLeft = parent.getPaddingLeft();
        int paddingTop = parent.getPaddingTop();
        int paddingRight = parent.getPaddingRight();
        int paddingBottom = parent.getPaddingBottom();
        
        int parentWidth = right - left;
        int parentHeight = bottom - top;
        int contentWidth = parentWidth - paddingLeft - paddingRight;
        int contentHeight = parentHeight - paddingTop - paddingBottom;
        
        // Create layout nodes
        List<LayoutNode> nodes = new ArrayList<>();
        Map<UIComponent, LayoutNode> nodeMap = new HashMap<>();
        
        for (UIComponent child : children) {
            LayoutParams lp = child.getLayoutParams();
            RelativeLayoutParams rlp = (lp instanceof RelativeLayoutParams) ? 
                (RelativeLayoutParams) lp : new RelativeLayoutParams();
            LayoutNode node = new LayoutNode(child, rlp);
            nodes.add(node);
            nodeMap.put(child, node);
        }
        
        // Sort nodes by dependency order
        List<LayoutNode> sortedNodes = topologicalSort(nodes);
        
        // Position children in dependency order
        for (LayoutNode node : sortedNodes) {
            positionChild(node, nodeMap, paddingLeft, paddingTop, contentWidth, contentHeight);
            node.positioned = true;
        }
    }
    
    private void positionChild(LayoutNode node, Map<UIComponent, LayoutNode> nodeMap,
                              int paddingLeft, int paddingTop, int contentWidth, int contentHeight) {
        UIComponent child = node.component;
        RelativeLayoutParams rlp = node.params;
        
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        
        // Default position (top-left)
        int childLeft = paddingLeft + rlp.marginLeft;
        int childTop = paddingTop + rlp.marginTop;
        int childRight = childLeft + childWidth;
        int childBottom = childTop + childHeight;
        
        // Apply horizontal positioning rules
        if (rlp.alignParentLeft) {
            childLeft = paddingLeft + rlp.marginLeft;
            childRight = childLeft + childWidth;
        } else if (rlp.alignParentRight) {
            childRight = paddingLeft + contentWidth - rlp.marginRight;
            childLeft = childRight - childWidth;
        } else if (rlp.centerHorizontal || rlp.centerInParent) {
            int availableWidth = contentWidth - rlp.getTotalMarginHorizontal();
            childLeft = paddingLeft + rlp.marginLeft + (availableWidth - childWidth) / 2;
            childRight = childLeft + childWidth;
        }
        
        // Apply relative horizontal positioning
        if (rlp.leftOf != null) {
            LayoutNode targetNode = nodeMap.get(rlp.leftOf);
            if (targetNode != null && targetNode.positioned) {
                childRight = targetNode.component.getLeft() - rlp.marginRight;
                childLeft = childRight - childWidth;
            }
        } else if (rlp.rightOf != null) {
            LayoutNode targetNode = nodeMap.get(rlp.rightOf);
            if (targetNode != null && targetNode.positioned) {
                childLeft = targetNode.component.getRight() + rlp.marginLeft;
                childRight = childLeft + childWidth;
            }
        }
        
        // Apply horizontal alignment
        if (rlp.alignLeft != null) {
            LayoutNode targetNode = nodeMap.get(rlp.alignLeft);
            if (targetNode != null && targetNode.positioned) {
                childLeft = targetNode.component.getLeft() + rlp.marginLeft;
                childRight = childLeft + childWidth;
            }
        } else if (rlp.alignRight != null) {
            LayoutNode targetNode = nodeMap.get(rlp.alignRight);
            if (targetNode != null && targetNode.positioned) {
                childRight = targetNode.component.getRight() - rlp.marginRight;
                childLeft = childRight - childWidth;
            }
        }
        
        // Apply vertical positioning rules
        if (rlp.alignParentTop) {
            childTop = paddingTop + rlp.marginTop;
            childBottom = childTop + childHeight;
        } else if (rlp.alignParentBottom) {
            childBottom = paddingTop + contentHeight - rlp.marginBottom;
            childTop = childBottom - childHeight;
        } else if (rlp.centerVertical || rlp.centerInParent) {
            int availableHeight = contentHeight - rlp.getTotalMarginVertical();
            childTop = paddingTop + rlp.marginTop + (availableHeight - childHeight) / 2;
            childBottom = childTop + childHeight;
        }
        
        // Apply relative vertical positioning
        if (rlp.above != null) {
            LayoutNode targetNode = nodeMap.get(rlp.above);
            if (targetNode != null && targetNode.positioned) {
                childBottom = targetNode.component.getTop() - rlp.marginBottom;
                childTop = childBottom - childHeight;
            }
        } else if (rlp.below != null) {
            LayoutNode targetNode = nodeMap.get(rlp.below);
            if (targetNode != null && targetNode.positioned) {
                childTop = targetNode.component.getBottom() + rlp.marginTop;
                childBottom = childTop + childHeight;
            }
        }
        
        // Apply vertical alignment
        if (rlp.alignTop != null) {
            LayoutNode targetNode = nodeMap.get(rlp.alignTop);
            if (targetNode != null && targetNode.positioned) {
                childTop = targetNode.component.getTop() + rlp.marginTop;
                childBottom = childTop + childHeight;
            }
        } else if (rlp.alignBottom != null) {
            LayoutNode targetNode = nodeMap.get(rlp.alignBottom);
            if (targetNode != null && targetNode.positioned) {
                childBottom = targetNode.component.getBottom() - rlp.marginBottom;
                childTop = childBottom - childHeight;
            }
        } else if (rlp.alignBaseline != null) {
            LayoutNode targetNode = nodeMap.get(rlp.alignBaseline);
            if (targetNode != null && targetNode.positioned) {
                // For simplicity, align to the same top position
                // In a real implementation, you'd calculate baseline positions
                childTop = targetNode.component.getTop() + rlp.marginTop;
                childBottom = childTop + childHeight;
            }
        }
        
        // Set final position
        child.layout(childLeft, childTop, childRight, childBottom);
    }
    
    /**
     * Perform topological sort to determine layout order based on dependencies
     */
    private List<LayoutNode> topologicalSort(List<LayoutNode> nodes) {
        List<LayoutNode> result = new ArrayList<>();
        Set<LayoutNode> visited = new HashSet<>();
        Set<LayoutNode> visiting = new HashSet<>();
        Map<UIComponent, LayoutNode> nodeMap = new HashMap<>();
        
        for (LayoutNode node : nodes) {
            nodeMap.put(node.component, node);
        }
        
        for (LayoutNode node : nodes) {
            if (!visited.contains(node)) {
                topologicalSortVisit(node, nodeMap, visited, visiting, result);
            }
        }
        
        return result;
    }
    
    private void topologicalSortVisit(LayoutNode node, Map<UIComponent, LayoutNode> nodeMap,
                                     Set<LayoutNode> visited, Set<LayoutNode> visiting, List<LayoutNode> result) {
        if (visiting.contains(node)) {
            // Circular dependency detected - ignore this dependency
            return;
        }
        
        if (visited.contains(node)) {
            return;
        }
        
        visiting.add(node);
        
        // Visit dependencies first
        Set<UIComponent> dependencies = node.params.getDependencies();
        for (UIComponent dependency : dependencies) {
            LayoutNode dependencyNode = nodeMap.get(dependency);
            if (dependencyNode != null) {
                topologicalSortVisit(dependencyNode, nodeMap, visited, visiting, result);
            }
        }
        
        visiting.remove(node);
        visited.add(node);
        result.add(node);
    }
    
    @Override
    public void calculateMinimumSize() {
        List<UIComponent> children = getVisibleChildren();
        
        int minWidth = parent.getPaddingLeft() + parent.getPaddingRight();
        int minHeight = parent.getPaddingTop() + parent.getPaddingBottom();
        
        // For relative layout, minimum size is the maximum extent of any child
        for (UIComponent child : children) {
            LayoutParams lp = child.getLayoutParams();
            minWidth = Math.max(minWidth, child.getMinWidth() + lp.getTotalMarginHorizontal() + 
                               parent.getPaddingLeft() + parent.getPaddingRight());
            minHeight = Math.max(minHeight, child.getMinHeight() + lp.getTotalMarginVertical() + 
                                parent.getPaddingTop() + parent.getPaddingBottom());
        }
        
        parent.setMinWidth(minWidth);
        parent.setMinHeight(minHeight);
    }
}