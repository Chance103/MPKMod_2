package io.github.kurrycat.mpkmod.gui.components;

import io.github.kurrycat.mpkmod.compatability.MCClasses.Renderer2D;
import io.github.kurrycat.mpkmod.util.*;

import java.awt.*;
import java.util.ArrayList;

public class ScrollableList<I extends ScrollableListItem<I>> extends Component implements MouseInputListener, MouseScrollListener {
    public Color backgroundColor = Color.DARK_GRAY;
    public ScrollBar<I> scrollBar;
    public ArrayList<I> items = new ArrayList<>();

    public ScrollableList(Vector2D pos, Vector2D size) {
        super(pos);
        this.setSize(size);
        scrollBar = new ScrollBar<>(this);
    }

    public void addItem(I item) {
        this.items.add(item);
    }

    public ArrayList<I> getItems() {
        return items;
    }

    public int getItemCount() {
        return this.items.size();
    }

    public I getItem(int index) {
        return getItems().get(index);
    }

    public void render(Vector2D mouse) {
        int h = 1;
        ArrayList<I> items = getItems();

        double itemWidth = getSize().getX() - 2;
        if (shouldRenderScrollbar()) itemWidth -= scrollBar.barWidth - 1;

        for (int i = 0; i < getItemCount(); i++) {
            I item = getItem(i);
            if (item == null) item = items.get(i);
            if (h - scrollBar.scrollAmount > -item.getHeight() && h - scrollBar.scrollAmount < getSize().getY())
                item.render(
                        i,
                        new Vector2D(getDisplayPos().getX() + 1, getDisplayPos().getY() + h - scrollBar.scrollAmount),
                        new Vector2D(itemWidth, item.getHeight()),
                        mouse
                );
            h += item.getHeight() + 1;
        }

        Renderer2D.drawHollowRect(getDisplayPos().add(1), getSize().sub(2), 1, Color.BLACK);
        if (shouldRenderScrollbar())
            scrollBar.render(mouse);
        drawTopCover(
                new Vector2D(getDisplayPos().getX(), 0),
                new Vector2D(getSize().getX(), getDisplayPos().getY())
        );
        drawBottomCover(
                new Vector2D(getDisplayPos().getX(), getDisplayPos().getY() + getSize().getY()),
                new Vector2D(getSize().getX(), Renderer2D.getScaledSize().getY() - (getDisplayPos().getY() + getSize().getY()) + 2)
        );
    }

    public Pair<I, Vector2D> getItemAndRelMousePosUnderMouse(Vector2D mouse) {
        double itemWidth = getSize().getX() - 2;
        if (shouldRenderScrollbar()) itemWidth -= scrollBar.barWidth - 1;
        if (mouse.getX() < getDisplayPos().getX() + 1 || mouse.getX() > getDisplayPos().getX() + itemWidth + 1)
            return null;

        double currY = mouse.getY() - 1 - getDisplayPos().getY() + scrollBar.scrollAmount;
        for (int i = 0; i < getItemCount(); i++) {
            I item = getItem(i);
            if (item == null) item = getItems().get(i);
            if (currY >= 0 && currY <= item.getHeight()) {
                return new Pair<>(item, new Vector2D(mouse.getX() - getDisplayPos().getX() - 1, currY));
            }
            currY -= item.getHeight() + 1;
        }
        return null;
    }

    public void drawTopCover(Vector2D pos, Vector2D size) {
        Renderer2D.drawRect(pos, size, Color.DARK_GRAY);
    }

    public void drawBottomCover(Vector2D pos, Vector2D size) {
        Renderer2D.drawRect(pos, size, Color.DARK_GRAY);
    }

    private boolean shouldRenderScrollbar() {
        return totalHeight() > getSize().getY() - 2;
    }

    public boolean handleMouseInput(Mouse.State state, Vector2D mousePos, Mouse.Button button) {
        if (shouldRenderScrollbar())
            scrollBar.handleMouseInput(state, mousePos, button);

        Pair<I, Vector2D> p = getItemAndRelMousePosUnderMouse(mousePos);
        if (p != null) {
            I item = p.first;
            Vector2D relMousePos = p.second;
            return item.handleMouseInput(state, mousePos, button);
        }

        return contains(mousePos);
    }

    public boolean handleMouseScroll(Vector2D mousePos, int delta) {
        Pair<I, Vector2D> p = getItemAndRelMousePosUnderMouse(mousePos);
        if (p != null) {
            I item = p.first;
            Vector2D relMousePos = p.second;
            return item.handleMouseScroll(mousePos, delta);
        }

        if (shouldRenderScrollbar())
            scrollBar.scrollBy(-delta);
        return contains(mousePos);
    }

    public int totalHeight() {
        if (getItemCount() == 0) return 0;

        int sum = 3;
        ArrayList<I> items = getItems();
        for (int i = 0; i < getItemCount(); i++) {
            if (getItem(i) != null) sum += getItem(i).getHeight() + 1;
            else sum += items.get(i).getHeight() + 1;
        }
        return sum;
    }

    public static class ScrollBar<I extends ScrollableListItem<I>> extends Component implements MouseInputListener {
        private final ScrollableList<I> parent;
        public double barWidth = 10;
        public Color backgroundColor = Color.DARK_GRAY;
        public Color hoverColor = new Color(180, 180, 180);
        public Color clickedColor = new Color(101, 101, 101);
        private int scrollAmount = 0;

        private int clickedYOffset = -1;

        public ScrollBar(ScrollableList<I> parent) {
            super(null);
            this.pos = parent.getDisplayPos().add(parent.getSize().getX() - barWidth, 0);
            this.setSize(new Vector2D(barWidth, parent.getSize().getY()));
            this.parent = parent;
        }

        @Override
        public void render(Vector2D mouse) {
            Renderer2D.drawRectWithEdge(getDisplayPos(), getSize(), 1, backgroundColor, Color.BLACK);
            BoundingBox2D scrollButtonBB = getScrollButtonBB();

            Renderer2D.drawRect(
                    scrollButtonBB.getMin().add(1),
                    scrollButtonBB.getSize().sub(2),
                    clickedYOffset != -1 ? clickedColor : contains(mouse) ? hoverColor : Color.WHITE
            );
        }

        public BoundingBox2D getScrollButtonBB() {
            return BoundingBox2D.fromPosSize(
                    new Vector2D(
                            getDisplayPos().getX() + 1,
                            getDisplayPos().getY() + mapScrollAmountToScrollButtonPos()
                    ),
                    new Vector2D(barWidth - 2, getScrollButtonHeight())
            );
        }

        public int mapScrollAmountToScrollButtonPos() {
            return MathUtil.map(
                    scrollAmount,
                    0, parent.totalHeight() - parent.getSize().getYI() - 2,
                    1, getSize().getYI() - getScrollButtonHeight() - 1
            );
        }

        public int mapScrollButtonPosToScrollAmount(Vector2D pos) {
            return MathUtil.map(
                    pos.getYI() - clickedYOffset - getDisplayPos().getYI(),
                    1, getSize().getYI() - getScrollButtonHeight() - 1,
                    0, parent.totalHeight() - parent.getSize().getYI() - 2
            );
        }

        @Override
        public boolean handleMouseInput(Mouse.State state, Vector2D mousePos, Mouse.Button button) {
            switch (state) {
                case DOWN:
                    if (getScrollButtonBB().contains(mousePos))
                        clickedYOffset = mousePos.getYI() - getScrollButtonBB().getMin().getYI();
                    break;
                case DRAG:
                    if (clickedYOffset != -1) {
                        scrollAmount = mapScrollButtonPosToScrollAmount(mousePos);
                        constrainScrollAmountToScreen();
                    }
                    break;
                case UP:
                    if (clickedYOffset != -1) {
                        scrollAmount = mapScrollButtonPosToScrollAmount(mousePos);
                        constrainScrollAmountToScreen();
                    }
                    clickedYOffset = -1;
                    break;
            }

            return getScrollButtonBB().contains(mousePos);
        }

        public void scrollBy(int delta) {
            scrollAmount += delta;
            constrainScrollAmountToScreen();
        }

        public int getScrollButtonHeight() {
            return Math.min(MathUtil.sqr(getSize().getYI() - 2) / parent.totalHeight(), getSize().getYI() - 2);
        }

        public void constrainScrollAmountToScreen() {
            scrollAmount = MathUtil.constrain(scrollAmount, 0, parent.totalHeight() - parent.getSize().getYI() - 2);
        }
    }
}
