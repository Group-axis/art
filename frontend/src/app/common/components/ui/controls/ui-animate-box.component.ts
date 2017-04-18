import {Component, Directive, ElementRef} from 'angular2/core';
import {AnimationBuilder} from '@angular/platform-browser/src/animate/animation_builder';

@Directive({
    selector: '[animate-box]',
    exportAs: 'animationBox'
})
class AnimateBox {
    private elementHeight: number;
    constructor(private aBuilder: AnimationBuilder, private elem: ElementRef) {
        this.elementHeight = this.elem.nativeElement.offsetHeight;
    }

    toggle(isVisible: boolean = false) {
        let animation = this.aBuilder.css();
        animation.setDuration(700);

        if (!isVisible) { // Goes up!
            animation.setFromStyles({ height: '0', overflow: 'hidden' })
                .setToStyles({ height: this.elementHeight + 'px' })
        } else { // Goes down!
            animation.setFromStyles({ height: this.elementHeight + 'px' })
                .setToStyles({ height: '0' })
        }
        animation.start(this.elem.nativeElement);
    }
}