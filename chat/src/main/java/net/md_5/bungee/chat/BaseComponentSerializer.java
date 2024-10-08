package net.md_5.bungee.chat;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentStyle;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Content;

import java.util.*;

public class BaseComponentSerializer
{

    protected void deserialize(JsonObject object, BaseComponent component, JsonDeserializationContext context)
    {
        component.applyStyle( context.deserialize( object, ComponentStyle.class ) );

        JsonElement insertion = object.get( "insertion" );
        if ( insertion != null )
        {
            component.setInsertion( insertion.getAsString() );
        }

        //Events
        JsonObject clickEvent = object.getAsJsonObject( "clickEvent" );
        if ( clickEvent != null )
        {
            component.setClickEvent( new ClickEvent(
                    ClickEvent.Action.valueOf( clickEvent.get( "action" ).getAsString().toUpperCase( Locale.ROOT ) ),
                    ( clickEvent.has( "value" ) ) ? clickEvent.get( "value" ).getAsString() : "" ) );
        }
        JsonObject hoverEventJson = object.getAsJsonObject( "hoverEvent" );
        if ( hoverEventJson != null )
        {
            HoverEvent hoverEvent = null;
            HoverEvent.Action action = HoverEvent.Action.valueOf( hoverEventJson.get( "action" ).getAsString().toUpperCase( Locale.ROOT ) );

            JsonElement value = hoverEventJson.get( "value" );
            if ( value != null )
            {

                // Plugins previously had support to pass BaseComponent[] into any action.
                // If the GSON is possible to be parsed as BaseComponent, attempt to parse as so.
                BaseComponent[] components;
                if ( value.isJsonArray() )
                {
                    components = context.deserialize( value, BaseComponent[].class );
                } else
                {
                    components = new BaseComponent[]
                    {
                        context.deserialize( value, BaseComponent.class )
                    };
                }
                hoverEvent = new HoverEvent( action, components );
            } else
            {
                JsonElement contents = hoverEventJson.get( "contents" );
                if ( contents != null )
                {
                    Content[] list;
                    if ( contents.isJsonArray() )
                    {
                        list = context.deserialize( contents, HoverEvent.getClass( action, true ) );
                    } else
                    {
                        list = new Content[]
                        {
                            context.deserialize( contents, HoverEvent.getClass( action, false ) )
                        };
                    }
                    hoverEvent = new HoverEvent( action, new ArrayList<>( Arrays.asList( list ) ) );
                }
            }

            if ( hoverEvent != null )
            {
                component.setHoverEvent( hoverEvent );
            }
        }

        JsonElement extra = object.get( "extra" );
        if ( extra != null )
        {
            component.setExtra( Arrays.asList( context.deserialize( extra, BaseComponent[].class ) ) );
        }
    }

    protected void serialize(JsonObject object, BaseComponent component, JsonSerializationContext context)
    {
        boolean first = false;
        if ( ComponentSerializer.serializedComponents.get() == null )
        {
            first = true;
            ComponentSerializer.serializedComponents.set( Collections.newSetFromMap( new IdentityHashMap<BaseComponent, Boolean>() ) );
        }
        try
        {
            Preconditions.checkArgument( !ComponentSerializer.serializedComponents.get().contains( component ), "Component loop" );
            ComponentSerializer.serializedComponents.get().add( component );

            ComponentStyleSerializer.serializeTo( component.getStyle(), object );

            if ( component.getInsertion() != null )
            {
                object.addProperty( "insertion", component.getInsertion() );
            }

            //Events
            if ( component.getClickEvent() != null )
            {
                JsonObject clickEvent = new JsonObject();
                clickEvent.addProperty( "action", component.getClickEvent().getAction().toString().toLowerCase( Locale.ROOT ) );
                clickEvent.addProperty( "value", component.getClickEvent().getValue() );
                object.add( "clickEvent", clickEvent );
            }
            if ( component.getHoverEvent() != null )
            {
                JsonObject hoverEvent = new JsonObject();
                hoverEvent.addProperty( "action", component.getHoverEvent().getAction().toString().toLowerCase( Locale.ROOT ) );
                if ( component.getHoverEvent().isLegacy() )
                {
                    hoverEvent.add( "value", context.serialize( component.getHoverEvent().getContents().get( 0 ) ) );
                } else
                {
                    hoverEvent.add( "contents", context.serialize( ( component.getHoverEvent().getContents().size() == 1 )
                            ? component.getHoverEvent().getContents().get( 0 ) : component.getHoverEvent().getContents() ) );
                }
                object.add( "hoverEvent", hoverEvent );
            }

            if ( component.getExtra() != null )
            {
                object.add( "extra", context.serialize( component.getExtra() ) );
            }
        } finally
        {
            ComponentSerializer.serializedComponents.get().remove( component );
            if ( first )
            {
                ComponentSerializer.serializedComponents.set( null );
            }
        }
    }
}
